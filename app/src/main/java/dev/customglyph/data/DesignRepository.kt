package dev.customglyph.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.AtomicFile
import dev.customglyph.imaging.MediaDecoder
import dev.customglyph.model.*
import java.io.*
import java.time.ZonedDateTime
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** App-private storage. Call file and commit operations on Dispatchers.IO. */
class DesignRepository(private val context: Context) {
    val prefs = context.getSharedPreferences("custom_glyph_v1", Context.MODE_PRIVATE)

    data class Imported(val file: String, val name: String, val media: SourceMedia)

    fun readDraft(): Draft =
        runCatching { DesignJson.draft(JSONObject(prefs.getString("draft", "{}")!!)) }
            .getOrDefault(Draft())

    fun saveDraft(draft: Draft) {
        commit { putString("draft", DesignJson.draft(draft).toString()) }
        cleanupSources(draft.image)
    }

    fun importMedia(uri: Uri): Imported {
        val temp = File.createTempFile("import-", ".bin", context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Cannot open this file." }
                temp.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var total = 0
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MediaDecoder.MAX_BYTES) {
                            "Choose a file smaller than 32 MB."
                        }
                        output.write(buffer, 0, count)
                    }
                }
            }
            val media = MediaDecoder.decode(temp)
            val name =
                runCatching {
                        context.contentResolver
                            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                            ?.use { if (it.moveToFirst()) it.getString(0) else null }
                    }
                    .getOrNull()
                    ?.take(100) ?: "Custom image"
            val file = "source-${UUID.randomUUID()}.bin"
            writeAtomic(file) { output -> temp.inputStream().use { it.copyTo(output) } }
            return Imported(file, name, media)
        } finally {
            temp.delete()
        }
    }

    fun loadMedia(file: String): SourceMedia = MediaDecoder.decode(privateFile(file))

    fun designs(): List<SavedDesign> = readArray("designs") { DesignJson.design(it) }

    fun rules(): List<ScheduleRule> = readArray("rules") { DesignJson.rule(it) }

    @Synchronized
    fun saveDesign(name: String, draft: Draft, sequence: FrameSequence): SavedDesign {
        val current = designs()
        require(current.count { !it.recent } < 30) {
            "Your library is full. Delete a design before saving another."
        }
        val file = writeSequence(sequence)
        val design =
            SavedDesign(
                name = name.trim().take(40).ifBlank { "Untitled" },
                draft = draft,
                frameFile = file,
            )
        commit {
            putString("designs", JSONArray((current + design).map(DesignJson::design)).toString())
        }
        return design
    }

    @Synchronized
    fun deleteDesign(id: String) {
        val remaining = designs().filterNot { it.id == id }
        commit {
            putString("designs", JSONArray(remaining.map(DesignJson::design)).toString())
            putString(
                "rules",
                JSONArray(rules().filterNot { it.designId == id }.map(DesignJson::rule)).toString(),
            )
        }
        cleanupSequences()
        cleanupSources(readDraft().image)
    }

    @Synchronized
    fun saveRule(rule: ScheduleRule) {
        require(designs().any { it.id == rule.designId }) {
            "Save the design before scheduling it."
        }
        val next = rules().filterNot { it.id == rule.id } + rule
        require(next.size <= 20) { "You can add up to 20 schedules." }
        val library = designs()
        val selected = library.first { it.id == rule.designId }
        require(!selected.recent || library.count { !it.recent } < 30) {
            "Save space in your gallery before scheduling this recent edit."
        }
        commit {
            putString("rules", JSONArray(next.map(DesignJson::rule)).toString())
            putString(
                "designs",
                JSONArray(
                        library
                            .map { if (it.id == rule.designId) it.copy(recent = false) else it }
                            .map(DesignJson::design)
                    )
                    .toString(),
            )
        }
    }

    @Synchronized
    fun deleteRule(id: String) {
        commit {
            putString(
                "rules",
                JSONArray(rules().filterNot { it.id == id }.map(DesignJson::rule)).toString(),
            )
        }
    }

    @Synchronized
    fun apply(draft: Draft, sequence: FrameSequence) {
        val file = writeSequence(sequence)
        val recent =
            SavedDesign(
                name = draft.name.substringBeforeLast('.'),
                draft = draft,
                frameFile = file,
                recent = true,
            )
        val updated =
            (designs() + recent).let { all ->
                all.filterNot { it.recent } + all.filter { it.recent }.takeLast(12)
            }
        commit {
            putString("applied", file)
            putBoolean("enabled", true)
            putLong("stop_at", 0)
            putString("designs", JSONArray(updated.map(DesignJson::design)).toString())
        }
        cleanupSequences()
    }

    @Synchronized
    fun recordRecent(draft: Draft, sequence: FrameSequence) {
        if (draft.image == null) return
        val current = designs()
        if (current.any { it.recent && it.draft == draft }) return
        val recent =
            SavedDesign(
                name = draft.name.substringBeforeLast('.'),
                draft = draft,
                frameFile = writeSequence(sequence),
                recent = true,
            )
        val updated =
            current.filterNot { it.recent } + (current.filter { it.recent } + recent).takeLast(12)
        commit { putString("designs", JSONArray(updated.map(DesignJson::design)).toString()) }
        cleanupSequences()
    }

    fun selectedFrameFile(now: ZonedDateTime = ZonedDateTime.now()): String? {
        val scheduled = if (schedulesEnabled) SchedulePolicy.select(rules(), now) else null
        return designs().firstOrNull { it.id == scheduled }?.frameFile
            ?: prefs.getString("applied", null)
    }

    fun readSequence(file: String): FrameSequence =
        DataInputStream(BufferedInputStream(privateFile(file).inputStream())).use { input ->
            require(input.readInt() == 0x43474c31) { "Saved design version is unsupported." }
            val profile =
                DeviceProfile.entries.getOrNull(input.readUnsignedByte())
                    ?: error("Invalid device profile.")
            val count = input.readUnsignedShort()
            require(count in 1..240)
            val frames = ArrayList<IntArray>(count)
            val delays = ArrayList<Int>(count)
            repeat(count) {
                delays += input.readInt().also { require(it in 83..30_000) }
                frames += IntArray(profile.size * profile.size) { input.readUnsignedByte() }
            }
            FrameSequence(profile, frames, delays)
        }

    private fun writeSequence(sequence: FrameSequence): String {
        val file = "frames-${UUID.randomUUID()}.cgl"
        writeAtomic(file) { raw ->
            val output = DataOutputStream(raw)
            output.writeInt(0x43474c31)
            output.writeByte(sequence.profile.ordinal)
            output.writeShort(sequence.frames.size)
            sequence.frames.forEachIndexed { index, pixels ->
                output.writeInt(sequence.delays[index])
                pixels.forEach { output.writeByte(it) }
            }
            output.flush()
        }
        return file
    }

    var enabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(value) {
            prefs.edit().putBoolean("enabled", value).apply()
        }

    var restoreOnBoot: Boolean
        get() = prefs.getBoolean("restore", true)
        set(value) {
            prefs.edit().putBoolean("restore", value).apply()
        }

    var onboarded: Boolean
        get() = prefs.getBoolean("onboarded", false)
        set(value) {
            prefs.edit().putBoolean("onboarded", value).apply()
        }

    var schedulesEnabled: Boolean
        get() = prefs.getBoolean("schedules", true)
        set(value) {
            prefs.edit().putBoolean("schedules", value).apply()
        }

    var showUnlocked: Boolean
        get() = prefs.getBoolean("show_unlocked", true)
        set(value) {
            prefs.edit().putBoolean("show_unlocked", value).apply()
        }

    var animate: Boolean
        get() = prefs.getBoolean("animate", true)
        set(value) {
            prefs.edit().putBoolean("animate", value).apply()
        }

    var haptics: Boolean
        get() = prefs.getBoolean("haptics", true)
        set(value) {
            prefs.edit().putBoolean("haptics", value).apply()
        }

    var stopAt: Long
        get() = prefs.getLong("stop_at", 0)
        set(value) {
            prefs.edit().putLong("stop_at", value).apply()
        }

    private inline fun commit(change: android.content.SharedPreferences.Editor.() -> Unit) {
        check(prefs.edit().apply(change).commit()) { "Could not save changes. Check free storage." }
    }

    private fun <T> readArray(key: String, parse: (JSONObject) -> T): List<T> =
        runCatching {
                val json = JSONArray(prefs.getString(key, "[]"))
                (0 until json.length()).map { parse(json.getJSONObject(it)) }
            }
            .getOrDefault(emptyList())

    private fun privateFile(name: String): File {
        require(name.matches(Regex("(?:source-[a-f0-9-]+\\.bin|frames-[a-f0-9-]+\\.cgl)"))) {
            "Invalid private file name."
        }
        return File(context.filesDir, name)
    }

    private fun writeAtomic(name: String, write: (FileOutputStream) -> Unit) {
        val file = AtomicFile(privateFile(name))
        val stream = file.startWrite()
        try {
            write(stream)
            file.finishWrite(stream)
        } catch (error: Exception) {
            file.failWrite(stream)
            throw error
        }
    }

    private fun cleanupSources(current: String?) {
        if (current == null) return
        val keep = designs().mapNotNull { it.draft.image }.toSet() + current
        val cutoff = privateFile(current).lastModified()
        context.filesDir
            .listFiles()
            ?.filter {
                it.name.startsWith("source-") && it.name !in keep && it.lastModified() < cutoff
            }
            ?.forEach { it.delete() }
    }

    private fun cleanupSequences() {
        val keep = designs().map { it.frameFile }.toSet() + prefs.getString("applied", null)
        context.filesDir
            .listFiles()
            ?.filter { it.name.startsWith("frames-") && it.name !in keep }
            ?.forEach { it.delete() }
    }
}
