@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.customglyph.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.customglyph.BuildConfig
import dev.customglyph.imaging.Adjustments
import dev.customglyph.imaging.MatrixRenderer
import dev.customglyph.model.DeviceProfile

@Composable
internal fun Onboarding(vm: EditorViewModel, profile: DeviceProfile, onDone: () -> Unit) {
    val device = vm.glyph.device
    var setup by rememberSaveable { mutableStateOf(false) }
    Dialog(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = Ink, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Column(
                    Modifier.weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DotText(
                            if (setup) "DISPLAY\nSETUP" else "CUSTOM\nGLYPH",
                            Modifier.weight(1f),
                            size = 44.sp,
                        )
                        Surface(
                            color = ActionRed,
                            shape = CircleShape,
                            modifier = Modifier.size(64.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Bolt,
                                    null,
                                    Modifier.size(28.dp),
                                    tint = Color.White,
                                )
                            }
                        }
                    }
                    if (setup) SetupChecklist(vm)
                    else {
                        val pixels =
                            remember(profile) {
                                MatrixRenderer.render(
                                    MatrixRenderer.sampleLogo(),
                                    Adjustments(),
                                    profile,
                                )
                            }
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            MatrixPreview(pixels, Modifier.weight(1f), profile = profile)
                            Column(
                                Modifier.width(104.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                LightWidget(Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(16.dp)) {
                                        DotText("${profile.size}", size = 40.sp)
                                        Text("× ${profile.size}", fontSize = 12.sp)
                                    }
                                }
                                Surface(color = Panel, shape = RoundedCornerShape(24.dp)) {
                                    Column(Modifier.padding(16.dp).fillMaxWidth()) {
                                        DotText("${profile.ledCount}", size = 32.sp)
                                        Text(
                                            "LEDs",
                                            fontSize = 10.sp,
                                            color = Muted,
                                            modifier = Modifier.padding(top = 4.dp),
                                        )
                                    }
                                }
                            }
                        }
                        Column(Modifier.padding(horizontal = 4.dp)) {
                            Text(device?.title ?: "Editor mode", fontSize = 18.sp)
                            Text(
                                when (device) {
                                    DeviceProfile.PHONE_3 ->
                                        "Detected · app display + Always-On toy"
                                    DeviceProfile.PHONE_4A_PRO -> "Detected · Always-On toy only"
                                    null ->
                                        "Edit and export here. Rear display needs Phone (3) or Phone (4a) Pro."
                                },
                                fontSize = 11.sp,
                                color = Muted,
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        Surface(color = Panel, shape = RoundedCornerShape(28.dp)) {
                            Column(Modifier.padding(20.dp).padding(bottom = 0.dp)) {
                                SetupStep("01", "Choose an image", "PNG, JPEG, WebP, SVG or GIF.")
                                SetupStep(
                                    "02",
                                    "Make it fit",
                                    "Pinch, drag, rotate. Adjust the light.",
                                )
                                SetupStep(
                                    "03",
                                    "Set up the display",
                                    if (device == null)
                                        "Display setup is available on a supported Nothing phone."
                                    else
                                        "Choose Custom Glyph as your Always-On toy. Review permissions on the next screen.",
                                )
                            }
                        }
                    }
                }
                Button(
                    onClick = { if (!setup && vm.glyph.supported) setup = true else onDone() },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = ActionRed,
                            contentColor = Color.White,
                        ),
                    shape = CircleShape,
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 16.dp)
                            .height(58.dp),
                ) {
                    Text(
                        if (!setup && vm.glyph.supported) "Set up display" else "Open editor",
                        Modifier.weight(1f),
                    )
                    Icon(Icons.Outlined.ArrowOutward, null, Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
internal fun SetupStep(number: String, title: String, body: String) {
    Row(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        DotText(
            number,
            color = Red,
            size = 22.sp,
            modifier = Modifier.width(36.dp).padding(top = 2.dp),
        )
        Column {
            Text(title, fontSize = 14.sp)
            Text(
                body,
                color = Muted,
                fontSize = 11.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
internal fun SettingsSheet(vm: EditorViewModel, onDismiss: () -> Unit, message: (String) -> Unit) {
    val context = LocalContext.current
    val status by vm.glyph.status.collectAsStateWithLifecycle()
    var details by remember { mutableStateOf(false) }
    var restore by remember { mutableStateOf(vm.repository.restoreOnBoot) }
    var animate by remember { mutableStateOf(vm.repository.animate) }
    var haptics by remember { mutableStateOf(vm.repository.haptics) }
    var enabled by remember { mutableStateOf(vm.repository.enabled) }
    var setup by remember { mutableStateOf(false) }
    var unlocked by remember { mutableStateOf(vm.repository.showUnlocked) }
    var licenses by remember { mutableStateOf(false) }
    fun launch(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            message("This screen is unavailable. Open system settings manually.")
        }
    }
    fun link(url: String) = launch(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Ink) {
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DotText("SETTINGS", Modifier.weight(1f), size = 34.sp)
                IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "Close settings") }
            }
            if (vm.glyph.supported) AlwaysOnToySetup()
            LightWidget(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        vm.glyph.device?.title ?: "${Build.MANUFACTURER} ${Build.MODEL}",
                        fontSize = 15.sp,
                    )
                    Text(
                        status.title,
                        fontSize = 11.sp,
                        color = if (status.error) ActionRed else Ink,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    Text(
                        status.detail,
                        fontSize = 10.sp,
                        lineHeight = 17.sp,
                        color = Color(0xff55545b),
                        modifier = Modifier.padding(top = 5.dp),
                    )
                    if (status.error)
                        TextButton(
                            onClick = {
                                context
                                    .getSystemService(ClipboardManager::class.java)
                                    .setPrimaryClip(
                                        ClipData.newPlainText(
                                            "Custom Glyph diagnostics",
                                            vm.glyph.diagnostics(),
                                        )
                                    )
                            }
                        ) {
                            Text("Copy error details", fontSize = 11.sp)
                        }
                    Button(
                        onClick = { setup = true },
                        modifier =
                            Modifier.fillMaxWidth().padding(top = 14.dp).heightIn(min = 48.dp),
                    ) {
                        Text("Review display setup", fontSize = 12.sp)
                    }
                }
            }
            if (vm.glyph.directDisplay)
                SettingSwitch(
                    "Show while unlocked",
                    "Face-down display uses the selected Always-On toy.",
                    unlocked,
                ) {
                    unlocked = it
                    vm.setShowUnlocked(it)
                }
            SettingSwitch("Animate GIFs", "Nothing OS may pause playback during sleep.", animate) {
                animate = it
                vm.repository.animate = it
            }
            SettingSwitch(
                "Restore after reboot",
                "Resumes after first unlock, if Android permits.",
                restore,
            ) {
                restore = it
                vm.repository.restoreOnBoot = it
            }
            SettingSwitch("Haptic feedback", "Vibration when applying a design.", haptics) {
                haptics = it
                vm.repository.haptics = it
            }
            Surface(color = Panel, shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    TextButton(
                        onClick = {
                            launch(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:${context.packageName}"),
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Permissions & battery", Modifier.weight(1f), fontSize = 12.sp)
                        Icon(Icons.Outlined.ArrowOutward, null, Modifier.size(18.dp))
                    }
                    TextButton(
                        onClick = { details = !details },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("How display behavior works", Modifier.weight(1f), fontSize = 11.sp)
                        Icon(
                            if (details) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            null,
                        )
                    }
                    if (details)
                        Text(
                            if (vm.glyph.device?.aodOnly == true)
                                "Phone (4a) Pro supports Always-On toys only. Select Custom Glyph in Settings → Glyph Interface → Flip to Glyph → Always-on Glyph Toy. Nothing OS controls when the toy runs."
                            else
                                "While unlocked, the app sends your design. On lock, it releases the app route; the selected Always-On toy handles face-down display. Locked and face up, the design turns off. Select Custom Glyph as your Always-On toy to replace the clock. Nothing OS can interrupt requests. Continuous illumination is not guaranteed, and sensors or timers may pause in deep sleep.",
                            fontSize = 11.sp,
                            color = Muted,
                            lineHeight = 19.sp,
                            modifier = Modifier.padding(12.dp),
                        )
                }
            }
            if (enabled)
                Button(
                    onClick = {
                        vm.stop()
                        enabled = false
                        message("Display stopped.")
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = ActionRed,
                            contentColor = Color.White,
                        ),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text("Stop display")
                }
            LightWidget(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        DotText("ABOUT", Modifier.weight(1f), size = 30.sp)
                        Text("v${BuildConfig.VERSION_NAME}", fontSize = 11.sp)
                    }
                    Text("Custom Glyph", fontSize = 15.sp, modifier = Modifier.padding(top = 16.dp))
                    Text(
                        "Tejas Agrawal",
                        fontSize = 11.sp,
                        color = Color(0xff55545b),
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    TextButton(
                        onClick = { link("https://x.com/PossiblyTejas") },
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text("@PossiblyTejas", fontSize = 12.sp)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Outlined.ArrowOutward, null, Modifier.size(16.dp))
                    }
                    Row {
                        TextButton(
                            onClick = { link("https://github.com/thetejasagrawal/CustomGlyph-") },
                            contentPadding = PaddingValues(end = 20.dp),
                        ) {
                            Text("Source code", fontSize = 11.sp)
                        }
                        TextButton(onClick = { licenses = true }) {
                            Text("Licenses", fontSize = 11.sp)
                        }
                    }
                    Text(
                        "Independent app. Not affiliated with Nothing.",
                        color = Color(0xff55545b),
                        fontSize = 10.sp,
                        lineHeight = 17.sp,
                    )
                }
            }
            Text(
                "Uninstalling removes your images and settings. Export anything you want to keep.",
                fontSize = 10.sp,
                color = Muted,
                lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
            )
        }
    }
    if (setup)
        Dialog(
            onDismissRequest = { setup = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(color = Ink, modifier = Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
                    DotText(
                        "Display setup",
                        size = 30.sp,
                        modifier = Modifier.padding(bottom = 20.dp),
                    )
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        SetupChecklist(vm)
                    }
                    Button(
                        onClick = { setup = false },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    if (licenses)
        AlertDialog(
            onDismissRequest = { licenses = false },
            title = { Text("Licenses") },
            text = {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        "Custom Glyph: MIT\nDoto, JetBrains Mono: SIL OFL 1.1\nAndroidX, Kotlin, AndroidSVG: Apache 2.0\nGlide GIF decoder: BSD / MIT / Apache 2.0\nNothing Glyph Matrix SDK: proprietary EULA",
                        fontSize = 12.sp,
                        lineHeight = 22.sp,
                    )
                    TextButton(
                        onClick = {
                            link(
                                "https://github.com/thetejasagrawal/CustomGlyph-/blob/main/THIRD_PARTY_NOTICES.md"
                            )
                        }
                    ) {
                        Text("Full notices ↗")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { licenses = false }) { Text("Done") } },
        )
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    value: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Surface(color = Panel, shape = RoundedCornerShape(28.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(end = 10.dp)) {
                Text(title, fontSize = 13.sp)
                Text(
                    description,
                    color = Muted,
                    fontSize = 10.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
            Switch(checked = value, onCheckedChange = onChange)
        }
    }
}
