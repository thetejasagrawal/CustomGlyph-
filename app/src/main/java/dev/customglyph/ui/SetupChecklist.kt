package dev.customglyph.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Checks observable permissions again on return. Toy selection is explicitly user-confirmed. */
// Background orientation tracking is a core function; this optional request is only user-initiated.
@SuppressLint("BatteryLife")
@Composable
internal fun SetupChecklist(vm: EditorViewModel) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var revision by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmedToy by remember {
        mutableStateOf(vm.repository.prefs.getBoolean("toy_selected_by_user", false))
    }
    val runtimePermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            revision++
        }
    val permission = "com.nothing.ketchum.permission.ENABLE"
    val oldApp = remember {
        runCatching { context.packageManager.getPackageInfo("dev.glyphstudio", 0) }.isSuccess
    }
    val info = remember {
        runCatching { context.packageManager.getPermissionInfo(permission, 0) }.getOrNull()
    }
    val dangerous = info?.protection == android.content.pm.PermissionInfo.PROTECTION_DANGEROUS
    val glyphGranted =
        remember(revision) {
            ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
        }
    val notified =
        remember(revision) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }
    val unrestricted =
        remember(revision) {
            context
                .getSystemService(PowerManager::class.java)
                .isIgnoringBatteryOptimizations(context.packageName)
        }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) revision++
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    fun open(intent: Intent) {
        try {
            context.startActivity(intent)
            error = null
        } catch (_: Exception) {
            error =
                "Open Settings → Glyph Interface manually. This firmware does not expose the shortcut."
            runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
        }
    }
    if (!vm.glyph.supported) {
        Text(
            "This device can edit and export. Setup for the rear display is available on Phone (3) and Phone (4a) Pro.",
            fontSize = 12.sp,
            lineHeight = 20.sp,
            color = Muted,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (oldApp)
            SetupCard(
                "Older app installed",
                "Stop or uninstall Glyph Studio before enabling Custom Glyph. Running both can cause competing display requests.",
                "Open older app settings",
            ) {
                open(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:dev.glyphstudio"),
                    )
                )
            }
        if (dangerous)
            SetupCard(
                "Glyph access",
                if (glyphGranted) "Allowed" else "Required to send images to the rear display.",
                if (glyphGranted) "Open permissions" else "Allow",
            ) {
                if (glyphGranted)
                    open(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}"),
                        )
                    )
                else runtimePermission.launch(permission)
            }
        if (vm.glyph.directDisplay) {
            SetupCard(
                "Notifications",
                if (notified) "Allowed"
                else "Shows the running display service and its Stop button.",
                if (notified) "Manage" else "Allow",
            ) {
                if (notified)
                    open(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                else runtimePermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            SetupCard(
                "Background battery use",
                if (unrestricted) "Unrestricted"
                else
                    "Allow background use for orientation detection. Android may still pause the app.",
                if (unrestricted) "Manage" else "Allow background use",
            ) {
                if (unrestricted)
                    open(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}"),
                        )
                    )
                else
                    open(
                        Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}"),
                        )
                    )
            }
        }
        SetupCard(
            "Always-On toy",
            "Choose Custom Glyph, replacing the clock: Settings → Glyph Interface → Flip to Glyph → Always-on Glyph Toy. Some firmware calls this Always-on Glyph.",
            "Open Glyph settings",
        ) {
            open(
                Intent()
                    .setComponent(
                        ComponentName(
                            "com.nothing.thirdparty",
                            "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity",
                        )
                    )
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = confirmedToy,
                onCheckedChange = {
                    confirmedToy = it
                    vm.repository.prefs.edit().putBoolean("toy_selected_by_user", it).apply()
                },
            )
            Column(Modifier.weight(1f)) {
                Text(
                    "I selected Custom Glyph as the Always-On toy",
                    fontSize = 11.sp,
                    lineHeight = 18.sp,
                )
                Text(
                    "Confirmed by you; Android does not expose this setting to the app.",
                    fontSize = 9.sp,
                    lineHeight = 15.sp,
                    color = Muted,
                )
            }
        }
        Text(
            "Place the phone face down to check. If the clock returns, reselect Custom Glyph as the Always-On toy. Switching toys with the Glyph Button does not change the Always-On selection.",
            fontSize = 11.sp,
            lineHeight = 18.sp,
            color = Muted,
        )
        error?.let { Text(it, color = Red, fontSize = 11.sp) }
    }
}

@Composable
private fun SetupCard(title: String, description: String, action: String, onClick: () -> Unit) {
    Surface(color = Panel, shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text(title, fontSize = 14.sp)
            Text(
                description,
                color = Muted,
                fontSize = 11.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text(action, fontSize = 11.sp)
            }
        }
    }
}
