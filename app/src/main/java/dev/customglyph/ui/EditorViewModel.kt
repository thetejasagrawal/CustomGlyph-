package dev.customglyph.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.customglyph.GlyphApplication
import dev.customglyph.glyph.DisplayService
import dev.customglyph.imaging.*
import dev.customglyph.model.*
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as GlyphApplication
    private val displayServiceIntent by lazy { Intent(app, DisplayService::class.java) }
    val repository = app.repository
    val glyph = app.glyph

    data class Editor(
        val draft: Draft = Draft(),
        val pixels: IntArray = IntArray(625),
        val busy: Boolean = true,
        val error: String? = null,
        val width: Int = 25,
        val height: Int = 25,
        val format: String = "SAMPLE",
        val frames: Int = 1,
        val frameIndex: Int = 0,
        val playing: Boolean = true,
    )

    private val mutable = MutableStateFlow(Editor())
    val editor = mutable.asStateFlow()
    private val libraryState = MutableStateFlow<List<SavedDesign>>(emptyList())
    val library = libraryState.asStateFlow()
    private val ruleState = MutableStateFlow<List<ScheduleRule>>(emptyList())
    val rules = ruleState.asStateFlow()
    private var source = SourceMedia(listOf(MatrixRenderer.sampleLogo()), listOf(1000), "SAMPLE")
    private var renderJob: Job? = null
    private var previewActive = false
    private val saves = Channel<Draft>(Channel.CONFLATED)
    val messages = Channel<String>(Channel.BUFFERED)

    init {
        viewModelScope.launch {
            for (draft in saves) {
                runCatching { withContext(Dispatchers.IO) { repository.saveDraft(draft) } }
                    .onFailure { messages.send(it.message ?: "Could not save changes.") }
            }
        }
        viewModelScope.launch {
            var draft = withContext(Dispatchers.IO) { repository.readDraft() }
            if (!repository.onboarded && glyph.device != null)
                draft = draft.copy(profile = glyph.device!!)
            val file = draft.image
            if (file != null) {
                runCatching { withContext(Dispatchers.IO) { repository.loadMedia(file) } }
                    .onSuccess { source = it }
                    .onFailure {
                        draft = Draft(profile = glyph.device ?: DeviceProfile.PHONE_3)
                        messages.send(it.message ?: "Choose your image again.")
                    }
            }
            setSource(draft, source)
            refreshLibrary()
            if (repository.prefs.getBoolean("resume_needed", false)) {
                messages.send("Android paused the display after reboot. Tap Apply to resume.")
                repository.prefs.edit().remove("resume_needed").apply()
            }
        }
    }

    fun setPreviewActive(active: Boolean) {
        previewActive = active
        renderPreview()
    }

    fun setPlaying(playing: Boolean) {
        mutable.update { it.copy(playing = playing) }
        renderPreview()
    }

    fun seek(frame: Int) {
        mutable.update {
            it.copy(frameIndex = frame.coerceIn(0, source.frames.lastIndex), playing = false)
        }
        renderPreview()
    }

    fun setProfile(profile: DeviceProfile) {
        if (mutable.value.busy) return
        val draft =
            mutable.value.draft.copy(
                profile = profile,
                adjustments = mutable.value.draft.adjustments.copy(x = 0f, y = 0f),
            )
        mutable.update { it.copy(draft = draft) }
        saves.trySend(draft)
        renderPreview()
    }

    fun update(change: (Adjustments) -> Adjustments) {
        if (mutable.value.busy) return
        val draft =
            mutable.value.draft.copy(
                adjustments = change(mutable.value.draft.adjustments).normalized()
            )
        mutable.update { it.copy(draft = draft, error = null) }
        saves.trySend(draft)
        renderPreview()
    }

    fun reset() = update { Adjustments(silhouette = it.silhouette) }

    private fun renderPreview() {
        renderJob?.cancel()
        if (mutable.value.busy) return
        renderJob =
            viewModelScope.launch {
                do {
                    val state = mutable.value
                    val index = state.frameIndex.coerceIn(source.frames.indices)
                    val pixels =
                        withContext(Dispatchers.Default) {
                            MatrixRenderer.render(
                                source.frames[index],
                                state.draft.adjustments,
                                state.draft.profile,
                            )
                        }
                    ensureActive()
                    mutable.update { it.copy(pixels = pixels) }
                    if (!previewActive || !state.playing || source.frames.size == 1) break
                    delay(source.delays[index].toLong())
                    mutable.update { it.copy(frameIndex = (index + 1) % source.frames.size) }
                } while (isActive)
            }
    }

    private suspend fun setSource(draft: Draft, media: SourceMedia) {
        source = media
        val raster = media.frames.first()
        val pixels =
            withContext(Dispatchers.Default) {
                MatrixRenderer.render(raster, draft.adjustments, draft.profile)
            }
        mutable.value =
            Editor(
                draft,
                pixels,
                false,
                width = raster.width,
                height = raster.height,
                format = media.format,
                frames = media.frames.size,
            )
        renderPreview()
    }

    fun import(uri: Uri) {
        if (mutable.value.busy) return
        renderJob?.cancel()
        mutable.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val imported = withContext(Dispatchers.IO) { repository.importMedia(uri) }
                val previous = mutable.value.draft
                if (previous.image != null) {
                    val previousFrames = renderSequence(previous)
                    withContext(Dispatchers.IO) {
                        repository.recordRecent(previous, previousFrames)
                    }
                    refreshLibrary()
                }
                val raster = imported.media.frames.first()
                val blackLogo =
                    withContext(Dispatchers.Default) {
                        var transparent = false
                        var count = 0
                        var total = 0L
                        for (pixel in raster.argb) {
                            if ((pixel ushr 24) < 250) transparent = true
                            if ((pixel ushr 24) > 128) {
                                count++
                                total +=
                                    (((pixel ushr 16) and 255) +
                                        ((pixel ushr 8) and 255) +
                                        (pixel and 255)) / 3
                            }
                        }
                        transparent && count > 0 && total / count < 100
                    }
                val draft =
                    Draft(
                        imported.file,
                        imported.name,
                        Adjustments(silhouette = blackLogo),
                        mutable.value.draft.profile,
                    )
                saves.send(draft)
                setSource(draft, imported.media)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutable.update {
                    it.copy(busy = false, error = error.message ?: "Could not read this image.")
                }
                renderPreview()
            }
        }
    }

    fun openDesign(design: SavedDesign) = operation {
        val media =
            withContext(Dispatchers.IO) { design.draft.image?.let(repository::loadMedia) }
                ?: SourceMedia(listOf(MatrixRenderer.sampleLogo()), listOf(1000), "SAMPLE")
        saves.send(design.draft)
        setSource(design.draft, media)
    }

    private suspend fun renderSequence(draft: Draft): FrameSequence =
        withContext(Dispatchers.Default) {
            FrameSequence(
                draft.profile,
                source.frames.map {
                    ensureActive()
                    MatrixRenderer.render(it, draft.adjustments, draft.profile)
                },
                source.delays,
            )
        }

    fun saveDesign(name: String) = operation {
        val draft = mutable.value.draft
        val sequence = renderSequence(draft)
        withContext(Dispatchers.IO) { repository.saveDesign(name, draft, sequence) }
        refreshLibrary()
        messages.send("Design saved.")
    }

    fun deleteDesign(id: String) = operation {
        withContext(Dispatchers.IO) { repository.deleteDesign(id) }
        refreshLibrary()
    }

    fun saveRule(rule: ScheduleRule) = operation {
        val design = libraryState.value.firstOrNull { it.id == rule.designId }
        if (glyph.supported && design?.draft?.profile != glyph.device) {
            messages.send("Choose a design made for ${glyph.device?.title}.")
            return@operation
        }
        withContext(Dispatchers.IO) { repository.saveRule(rule) }
        refreshLibrary()
        messages.send("Schedule saved. Tap Apply to enable display.")
    }

    fun deleteRule(id: String) = operation {
        withContext(Dispatchers.IO) { repository.deleteRule(id) }
        refreshLibrary()
    }

    private suspend fun refreshLibrary() {
        libraryState.value = withContext(Dispatchers.IO) { repository.designs() }
        ruleState.value = withContext(Dispatchers.IO) { repository.rules() }
    }

    fun applyDesign() = operation {
        val draft = mutable.value.draft
        if (glyph.supported && draft.profile != glyph.device) {
            messages.send("Select ${glyph.device?.title} before applying to this phone.")
            return@operation
        }
        val sequence = renderSequence(draft)
        withContext(Dispatchers.IO) { repository.apply(draft, sequence) }
        refreshLibrary()
        glyph.cancelPreview()
        when {
            glyph.directDisplay && repository.showUnlocked -> {
                DisplayService.start(app, true)
                messages.send("Design saved. Requesting Glyph display…")
            }
            glyph.supported -> {
                glyph.refresh(true)
                messages.send("Select Custom Glyph as your Always-on Glyph Toy in system settings.")
            }
            else -> messages.send("Design saved. Rear display requires a supported Nothing phone.")
        }
    }

    fun previewOnPhone() {
        if (!glyph.directDisplay || mutable.value.draft.profile != glyph.device) {
            viewModelScope.launch {
                messages.send(
                    "Direct preview is available on Phone (3). Use Always-on Glyph Toy on Phone (4a) Pro."
                )
            }
            return
        }
        val state = mutable.value
        operation { glyph.preview(renderCurrentFrame(state)) }
    }

    fun stop() {
        repository.enabled = false
        repository.stopAt = 0
        glyph.cancelPreview()
        glyph.refresh()
        app.stopService(displayServiceIntent)
    }

    fun setShowUnlocked(value: Boolean) {
        repository.showUnlocked = value
        glyph.refresh(true)
        if (!value) app.stopService(displayServiceIntent)
        else if (glyph.directDisplay && repository.enabled) {
            runCatching { DisplayService.start(app) }
                .onFailure {
                    glyph.report("Resume needed", "Tap Apply to restart the display service.", true)
                }
        }
    }

    fun setStopTimer(minutes: Int) {
        repository.stopAt = if (minutes <= 0) 0 else System.currentTimeMillis() + minutes * 60_000L
    }

    private suspend fun renderCurrentFrame(state: Editor): IntArray =
        withContext(Dispatchers.Default) {
            MatrixRenderer.render(
                source.frames[state.frameIndex.coerceIn(source.frames.indices)],
                state.draft.adjustments,
                state.draft.profile,
            )
        }

    fun export(uri: Uri, svg: Boolean, artwork: Boolean = false) {
        val state = mutable.value
        operation {
            val pixels = renderCurrentFrame(state)
            withContext(Dispatchers.IO) {
                app.contentResolver.openOutputStream(uri, "wt").use { output ->
                    requireNotNull(output) { "Cannot write to this location." }
                    if (svg)
                        output.write(MatrixExport.svg(pixels, state.draft.profile).toByteArray())
                    else MatrixExport.png(pixels, state.draft.profile, output, artwork)
                }
            }
            messages.send("Export complete.")
        }
    }

    fun share() {
        val state = mutable.value
        operation {
            val pixels = renderCurrentFrame(state)
            val uri =
                withContext(Dispatchers.IO) {
                    val directory = File(app.cacheDir, "shared").apply { mkdirs() }
                    directory
                        .listFiles()
                        ?.filter { System.currentTimeMillis() - it.lastModified() > 86_400_000L }
                        ?.forEach { it.delete() }
                    val file = File(directory, "custom-glyph-${System.currentTimeMillis()}.png")
                    file.outputStream().use {
                        MatrixExport.png(pixels, state.draft.profile, it, true)
                    }
                    FileProvider.getUriForFile(app, "${app.packageName}.files", file)
                }
            val intent =
                Intent(Intent.ACTION_SEND)
                    .setType("image/png")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            app.startActivity(
                Intent.createChooser(intent, "Share design").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun operation(block: suspend () -> Unit) {
        if (mutable.value.busy) return
        renderJob?.cancel()
        mutable.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                block()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                messages.send(error.message ?: "Could not complete this action.")
            } finally {
                mutable.update { it.copy(busy = false) }
                renderPreview()
            }
        }
    }
}
