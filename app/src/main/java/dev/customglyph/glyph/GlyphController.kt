package dev.customglyph.glyph

import android.app.KeyguardManager
import android.content.*
import android.os.*
import android.util.Log
import androidx.core.content.ContextCompat
import com.nothing.ketchum.*
import dev.customglyph.BuildConfig
import dev.customglyph.data.DesignRepository
import dev.customglyph.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One owner for the SDK singleton. All public methods run on the main thread. */
class GlyphController(private val context: Context, private val repository: DesignRepository) {
    data class Status(
        val title: String,
        val detail: String,
        val error: Boolean = false,
        val connected: Boolean = false,
    )

    val device: DeviceProfile? =
        runCatching {
                when {
                    Common.is23112() -> DeviceProfile.PHONE_3
                    Common.is25111p() -> DeviceProfile.PHONE_4A_PRO
                    else -> null
                }
            }
            .getOrNull()
    val supported: Boolean
        get() = device != null

    val directDisplay: Boolean
        get() = device == DeviceProfile.PHONE_3

    private val mutableStatus =
        MutableStateFlow(
            when (device) {
                DeviceProfile.PHONE_3 -> Status("Ready", "Apply a design to connect.")
                DeviceProfile.PHONE_4A_PRO ->
                    Status(
                        "Always-On setup required",
                        "Select Custom Glyph as your Always-on Glyph Toy in system settings.",
                    )
                null -> Status("Editor mode", "Rear display requires Phone (3) or Phone (4a) Pro.")
            }
        )
    val status = mutableStatus.asStateFlow()
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val owners = mutableSetOf<String>()
    private var manager: GlyphMatrixManager? = null
    private var connected = false
    private var generation = 0
    private var reconnectAttempts = 0
    private val reconnect = Runnable { if (owners.isNotEmpty() && !connected) reconnect() }
    private var aodEvent = false
    private var sentRoute: String? = null
    private var sentPixels: IntArray? = null
    private var cachedPixels: IntArray? = null
    private var cachedFrame: IntArray? = null
    private var failedRoute: String? = null
    private var lastFailure: String? = null
    private var sequenceFile: String? = null
    private var sequence: FrameSequence? = null
    private var sequenceJob: Job? = null
    private var playbackStart = SystemClock.elapsedRealtime()
    private var previewPixels: IntArray? = null
    private val toyActive: Boolean
        get() = TOY in owners

    private val sensor = OrientationMonitor(context) { refresh() }

    private val timeout = Runnable {
        if (!connected && owners.isNotEmpty()) {
            report(
                "Connection unavailable",
                "Enable Glyph Interface and check Glyph permission. Nothing's service did not connect.",
                true,
            )
            scheduleReconnect()
        }
    }
    private val endPreview = Runnable {
        previewPixels = null
        detach(PREVIEW)
    }
    private val tick =
        object : Runnable {
            override fun run() {
                if (owners.isEmpty()) return
                refresh()
                val animating =
                    repository.animate && (sequence?.frames?.size ?: 0) > 1 && canAnimate()
                handler.postDelayed(this, if (animating) 83 else 1000)
            }
        }
    private val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_USER_PRESENT) aodEvent = false
                refresh()
                handler.postDelayed({ if (owners.isNotEmpty()) refresh() }, 250)
            }
        }
    private val listener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (
                key in
                    setOf(
                        "enabled",
                        "applied",
                        "rules",
                        "designs",
                        "schedules",
                        "animate",
                        "stop_at",
                        "show_unlocked",
                    )
            )
                handler.post { refresh() }
        }

    fun attach(owner: String) {
        if (!supported || (owner != TOY && !directDisplay)) return
        if (!owners.add(owner)) {
            if (connected) refresh(true) else retry()
            return
        }
        if (owners.size == 1) {
            sensor.start()
            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(Intent.ACTION_SCREEN_ON)
                    addAction(Intent.ACTION_USER_PRESENT)
                    addAction(Intent.ACTION_TIME_CHANGED)
                    addAction(Intent.ACTION_TIMEZONE_CHANGED)
                    addAction(Intent.ACTION_DATE_CHANGED)
                },
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            repository.prefs.registerOnSharedPreferenceChangeListener(listener)
            connect()
            handler.post(tick)
        } else refresh(true)
    }

    private fun connect() {
        val connection = ++generation
        report("Connecting", "Waiting for Nothing's Glyph service…")
        try {
            manager = GlyphMatrixManager.getInstance(context)
            manager?.init(
                object : GlyphMatrixManager.Callback {
                    override fun onServiceConnected(componentName: ComponentName?) {
                        handler.post {
                            if (owners.isEmpty() || connection != generation) return@post
                            try {
                                val target =
                                    if (device == DeviceProfile.PHONE_4A_PRO) Glyph.DEVICE_25111p
                                    else Glyph.DEVICE_23112
                                connected = manager?.register(target) == true
                                handler.removeCallbacks(timeout)
                                if (connected) {
                                    reconnectAttempts = 0
                                    handler.removeCallbacks(reconnect)
                                    refresh(true)
                                } else
                                    report(
                                        "Registration refused",
                                        "Check Glyph access and Nothing OS. Your firmware may require an approved API key.",
                                        true,
                                    )
                            } catch (error: Exception) {
                                fail("Register with Glyph service", error)
                            }
                        }
                    }

                    override fun onServiceDisconnected(componentName: ComponentName?) {
                        handler.post {
                            if (owners.isEmpty() || connection != generation) return@post
                            connected = false
                            sentRoute = null
                            sentPixels = null
                            report("Glyph disconnected", "Reconnecting to Nothing’s service…", true)
                            scheduleReconnect()
                        }
                    }
                }
            )
            handler.postDelayed(timeout, 8000)
        } catch (error: Exception) {
            fail("Connect to Glyph service", error)
        }
    }

    fun retry() {
        reconnectAttempts = 0
        handler.removeCallbacks(reconnect)
        reconnect()
    }

    private fun scheduleReconnect() {
        if (reconnectAttempts >= 3 || owners.isEmpty()) return
        val delay = longArrayOf(1000, 3000, 10_000)[reconnectAttempts++]
        handler.removeCallbacks(reconnect)
        handler.postDelayed(reconnect, delay)
    }

    private fun reconnect() {
        if (owners.isEmpty()) return
        if (connected) refresh(true)
        else {
            handler.removeCallbacks(timeout)
            generation++
            runCatching { manager?.unInit() }
            manager = null
            connect()
        }
    }

    fun aodTick() {
        aodEvent = true
        refresh(true)
    }

    fun preview(pixels: IntArray) {
        if (!directDisplay) return
        require(pixels.size == device!!.size * device.size)
        previewPixels = pixels.copyOf()
        attach(PREVIEW)
        refresh(true)
        handler.removeCallbacks(endPreview)
        handler.postDelayed(endPreview, 5000)
    }

    fun cancelPreview() {
        handler.removeCallbacks(endPreview)
        previewPixels = null
        detach(PREVIEW)
    }

    fun detach(owner: String) {
        if (!owners.remove(owner)) return
        if (owner == TOY) {
            if (sentRoute == TOY) runCatching { manager?.turnOff() }
            aodEvent = false
            sentRoute = null
            sentPixels = null
        }
        if (owners.isNotEmpty()) {
            refresh(true)
            return
        }
        handler.removeCallbacks(timeout)
        handler.removeCallbacks(endPreview)
        handler.removeCallbacks(tick)
        handler.removeCallbacks(reconnect)
        previewPixels = null
        clearDisplay()
        generation++
        runCatching { manager?.unInit() }
        manager = null
        connected = false
        cachedFrame = null
        cachedPixels = null
        failedRoute = null
        sequenceJob?.cancel()
        sequence = null
        sequenceFile = null
        sensor.stop()
        runCatching { context.unregisterReceiver(receiver) }
        repository.prefs.unregisterOnSharedPreferenceChangeListener(listener)
        report("Display stopped", "Apply a design to resume.")
    }

    fun refresh(force: Boolean = false) {
        if (SchedulePolicy.expired(repository.stopAt, System.currentTimeMillis())) {
            repository.enabled = false
            repository.stopAt = 0
        }
        if (!connected) return
        val selected = repository.selectedFrameFile()
        if (selected != sequenceFile) loadSequence(selected)
        val locked = context.getSystemService(KeyguardManager::class.java).isKeyguardLocked
        val interactive = context.getSystemService(PowerManager::class.java).isInteractive
        val down = effectiveFaceDown()
        val active = previewPixels != null || repository.enabled
        val index =
            if (repository.animate && canAnimate())
                sequence?.indexAt(SystemClock.elapsedRealtime() - playbackStart) ?: 0
            else 0
        val pixels = previewPixels ?: sequence?.frames?.get(index)
        val selectedRoute =
            DisplayPolicy.route(
                toyActive,
                directDisplay,
                repository.showUnlocked || previewPixels != null,
                interactive,
                locked,
            )
        val ownsRoute = selectedRoute != null
        val shouldShow =
            ownsRoute &&
                pixels != null &&
                DisplayPolicy.shouldDisplay(active, interactive, locked, down) &&
                (directDisplay || toyActive)
        if (!shouldShow) {
            if (!clearDisplay()) return
            report(
                if (!active) "Display paused" else "Waiting",
                if (!active) "Apply a design to resume."
                else
                    "Locked display is handled by the selected Always-On toy. Check display setup if the clock appears.",
                connected = true,
            )
            return
        }
        if (previewPixels == null && sequence?.profile != device) {
            clearDisplay()
            report(
                "Different device profile",
                "Open the design and select ${device?.title} before applying it.",
                true,
            )
            return
        }
        val route = if (selectedRoute == DisplayPolicy.Route.TOY) TOY else APP
        // A failed GIF must not resubmit the same rejected request 12 times per second.
        // Apply / Retry, a fresh connection and OS AOD callbacks explicitly retry with force=true.
        if (!force && failedRoute == route) return
        if (!force && sentRoute == route && sentPixels?.contentEquals(pixels!!) == true) return
        var operation = "Switch to Glyph Toy"
        try {
            if (route == TOY && sentRoute == APP) manager?.closeAppMatrix()
            if (cachedPixels?.contentEquals(pixels!!) != true) {
                operation = "Render Glyph image"
                cachedFrame = GlyphFrameEncoder.encode(context, requireNotNull(device), pixels!!)
                cachedPixels = pixels.copyOf()
            }
            // Preserve ownership on failure so lock / Stop can still clear a partially accepted
            // request.
            sentRoute = route
            operation = if (route == TOY) "Send Glyph Toy frame" else "Send app Glyph frame"
            if (route == TOY) manager!!.setMatrixFrame(cachedFrame!!)
            else manager!!.setAppMatrixFrame(cachedFrame!!)
            failedRoute = null
            sentPixels = pixels!!.copyOf()
            report(
                if (route == TOY) "Submitted to Glyph Toy" else "Submitted to Glyph",
                "Nothing OS controls display availability. Check the rear display.",
                connected = true,
            )
        } catch (error: Exception) {
            sentPixels = null
            failedRoute = route
            fail(operation, error)
        }
    }

    private fun loadSequence(file: String?) {
        sequenceFile = file
        sequence = null
        sequenceJob?.cancel()
        if (file == null) return
        sequenceJob =
            scope.launch {
                try {
                    val loaded = withContext(Dispatchers.IO) { repository.readSequence(file) }
                    ensureActive()
                    if (sequenceFile == file) {
                        sequence = loaded
                        playbackStart = SystemClock.elapsedRealtime()
                        refresh(true)
                    }
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    report(
                        "Design unavailable",
                        "Open the editor and apply the design again.",
                        true,
                    )
                }
            }
    }

    private fun canAnimate(): Boolean {
        val unlocked =
            directDisplay &&
                context.getSystemService(PowerManager::class.java).isInteractive &&
                !context.getSystemService(KeyguardManager::class.java).isKeyguardLocked
        // No wake lock: playback pauses naturally when Nothing OS suspends the toy.
        return unlocked || (toyActive && effectiveFaceDown())
    }

    private fun effectiveFaceDown(): Boolean =
        DisplayPolicy.faceDown(sensor.fresh, sensor.faceDown, toyActive && aodEvent)

    private fun clearDisplay(): Boolean =
        try {
            when (sentRoute) {
                TOY -> manager?.turnOff()
                APP -> manager?.closeAppMatrix()
            }
            sentRoute = null
            sentPixels = null
            true
        } catch (error: Exception) {
            fail("Clear Glyph display", error)
            false
        }

    fun diagnostics(): String = buildString {
        appendLine("Custom Glyph ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Android: ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}")
        appendLine("OS build: ${Build.DISPLAY}")
        appendLine("Profile: ${device?.title ?: "unsupported"}")
        appendLine("SDK matrix size: ${runCatching { Common.getDeviceMatrixLength() }.getOrNull()}")
        appendLine("Registered: $connected; Toy bound: $toyActive; AOD event: $aodEvent")
        appendLine("Route: ${sentRoute ?: "none"}; owners: ${owners.sorted().joinToString()}")
        appendLine("Status: ${status.value.title}: ${status.value.detail}")
        lastFailure?.let { appendLine("Last failure:\n$it") }
    }

    fun report(
        title: String,
        detail: String,
        error: Boolean = false,
        connected: Boolean = this.connected,
    ) {
        mutableStatus.value = Status(title, detail, error, connected)
    }

    private fun fail(operation: String, error: Exception) {
        lastFailure = "$operation\n${Log.getStackTraceString(error)}"
        Log.e("CustomGlyph", operation, error)
        report(
            if (error is SecurityException) "Glyph access denied" else "Glyph request failed",
            "$operation: ${error.javaClass.simpleName}: ${error.message ?: "No detail supplied by the SDK."}",
            true,
        )
    }

    companion object {
        const val APP = "app"
        const val TOY = "toy"
        const val PREVIEW = "preview"
    }
}
