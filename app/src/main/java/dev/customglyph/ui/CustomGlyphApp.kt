@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.customglyph.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.*
import kotlinx.coroutines.launch

@Composable
fun CustomGlyphApp(vm: EditorViewModel) = GlyphTheme {
    val editor by vm.editor.collectAsStateWithLifecycle()
    val status by vm.glyph.status.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val largeText = LocalDensity.current.fontScale > 1.3f
    val landscape =
        with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() > 650.dp }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var onboarded by rememberSaveable { mutableStateOf(vm.repository.onboarded) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var preview by rememberSaveable { mutableStateOf(false) }
    var saving by rememberSaveable { mutableStateOf(false) }
    var exportMenu by remember { mutableStateOf(false) }
    var saveName by rememberSaveable { mutableStateOf("") }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
            it?.let(vm::import)
        }
    val exportPng =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) {
            it?.let { uri -> vm.export(uri, false) }
        }
    val exportSvg =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/svg+xml")) {
            it?.let { uri -> vm.export(uri, true) }
        }
    val exportArt =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) {
            it?.let { uri -> vm.export(uri, false, true) }
        }
    val notifications =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            vm.applyDesign()
        }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                if (
                    vm.glyph.directDisplay &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
                )
                    notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                else vm.applyDesign()
            } else
                scope.launch {
                    snackbar.showSnackbar("Enable Glyph permission in app settings to continue.")
                }
        }
    fun apply() {
        if (vm.repository.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val glyphPermission = "com.nothing.ketchum.permission.ENABLE"
        val info =
            runCatching { context.packageManager.getPermissionInfo(glyphPermission, 0) }.getOrNull()
        when {
            vm.glyph.supported &&
                info?.protection == android.content.pm.PermissionInfo.PROTECTION_DANGEROUS &&
                ContextCompat.checkSelfPermission(context, glyphPermission) !=
                    PackageManager.PERMISSION_GRANTED -> permission.launch(glyphPermission)
            vm.glyph.directDisplay &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED ->
                notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            else -> vm.applyDesign()
        }
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            vm.setPreviewActive(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        }
        lifecycle.addObserver(observer)
        vm.setPreviewActive(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        onDispose {
            lifecycle.removeObserver(observer)
            vm.setPreviewActive(false)
        }
    }
    LaunchedEffect(vm) { for (message in vm.messages) snackbar.showSnackbar(message) }
    Scaffold(
        containerColor = Ink,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Row(
                Modifier.statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    DotText("CUSTOM GLYPH", size = if (landscape) 25.sp else 30.sp)
                    if (!landscape)
                        Text(
                            vm.glyph.device?.title ?: "Editor mode",
                            fontSize = 10.sp,
                            color = Muted,
                            modifier = Modifier.padding(top = 5.dp),
                        )
                }
                Box(Modifier.width(64.dp), contentAlignment = Alignment.Center) {
                    FilledIconButton(
                        onClick = { settings = true },
                        shape = CircleShape,
                        colors =
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor = Panel,
                                contentColor = Paper,
                            ),
                        modifier = Modifier.size(52.dp),
                    ) {
                        Icon(Icons.Outlined.Settings, "Settings", Modifier.size(22.dp))
                    }
                }
            }
        },
        bottomBar = {
            val actions: @Composable (Modifier) -> Unit = { modifier ->
                Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = ::apply,
                        enabled = !editor.busy,
                        shape = CircleShape,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = ActionRed,
                                contentColor = Color.White,
                            ),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    ) {
                        Icon(Icons.Outlined.Bolt, null, Modifier.size(22.dp))
                        Text(
                            "Apply to Glyph",
                            Modifier.weight(1f).padding(horizontal = 12.dp),
                            fontSize = 13.sp,
                        )
                        Icon(Icons.Outlined.ArrowOutward, null, Modifier.size(19.dp))
                    }
                    CircleAction(
                        "Save",
                        Icons.Outlined.BookmarkBorder,
                        {
                            saveName = editor.draft.name.substringBeforeLast('.').take(40)
                            saving = true
                        },
                        Modifier.width(64.dp),
                        light = true,
                        enabled = !editor.busy,
                    )
                }
            }
            val navigation: @Composable (Modifier) -> Unit = { modifier ->
                Surface(color = Panel, shape = CircleShape, modifier = modifier) {
                    Row(
                        Modifier.padding(5.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        listOf(
                                "Editor" to Icons.Outlined.Tune,
                                "Gallery" to Icons.Outlined.GridView,
                                "Schedule" to Icons.Outlined.Schedule,
                            )
                            .forEachIndexed { index, (label, icon) ->
                                Surface(
                                    onClick = { tab = index },
                                    color = if (tab == index) Paper else Color.Transparent,
                                    contentColor = if (tab == index) Ink else Muted,
                                    shape = CircleShape,
                                    modifier =
                                        Modifier.weight(1f).height(48.dp).semantics {
                                            selected = tab == index
                                            role = Role.Tab
                                            contentDescription = label
                                        },
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        Icon(icon, null, Modifier.size(18.dp))
                                        if (!largeText) {
                                            Spacer(Modifier.width(6.dp))
                                            Text(label, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                    }
                }
            }
            if (landscape)
                Row(
                    Modifier.navigationBarsPadding().padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (tab == 0) actions(Modifier.weight(1f))
                    navigation(Modifier.weight(1f))
                }
            else
                Column(
                    Modifier.navigationBarsPadding().padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (tab == 0) actions(Modifier.fillMaxWidth())
                    navigation(Modifier.fillMaxWidth())
                }
        },
    ) { padding ->
        Crossfade(
            targetState = tab,
            label = "Navigation",
            modifier = Modifier.fillMaxSize().padding(padding),
        ) { page ->
            when (page) {
                1 -> LibraryScreen(vm) { tab = 0 }
                2 -> ScheduleScreen(vm)
                else ->
                    EditorWorkspace(
                        editor,
                        vm,
                        onChoose = { picker.launch(arrayOf("image/*", "image/svg+xml")) },
                        onPreview = { preview = true },
                        onSettings = { settings = true },
                        onExport = { exportMenu = true },
                        exportMenu = {
                            DropdownMenu(
                                expanded = exportMenu,
                                onDismissRequest = { exportMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Pixel map · PNG") },
                                    onClick = {
                                        exportMenu = false
                                        exportPng.launch("custom-glyph.png")
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Pixel map · SVG") },
                                    onClick = {
                                        exportMenu = false
                                        exportSvg.launch("custom-glyph.svg")
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Display artwork · PNG") },
                                    onClick = {
                                        exportMenu = false
                                        exportArt.launch("custom-glyph-artwork.png")
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Share image") },
                                    onClick = {
                                        exportMenu = false
                                        vm.share()
                                    },
                                    leadingIcon = { Icon(Icons.Outlined.Share, null) },
                                )
                            }
                        },
                    )
            }
        }
    }
    if (!onboarded)
        Onboarding(vm, editor.draft.profile) {
            vm.repository.onboarded = true
            onboarded = true
        }
    if (settings)
        SettingsSheet(
            vm,
            onDismiss = { settings = false },
            message = { scope.launch { snackbar.showSnackbar(it) } },
        )
    if (saving)
        AlertDialog(
            onDismissRequest = { saving = false },
            title = { Text("Save design") },
            text = {
                OutlinedTextField(
                    saveName,
                    { saveName = it.take(40) },
                    label = { Text("Name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.saveDesign(saveName)
                        saving = false
                    },
                    enabled = saveName.isNotBlank(),
                ) {
                    Text("Save")
                }
            },
            dismissButton = { TextButton(onClick = { saving = false }) { Text("Cancel") } },
        )
    if (preview)
        ModalBottomSheet(onDismissRequest = { preview = false }, containerColor = Ink) {
            Column(
                Modifier.fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DotText("PREVIEW", size = 28.sp)
                MatrixPreview(
                    editor.pixels,
                    Modifier.widthIn(max = 360.dp).fillMaxWidth().padding(vertical = 20.dp),
                    profile = editor.draft.profile,
                )
                Text(
                    "${editor.draft.profile.title} · ${editor.draft.profile.ledCount} LEDs",
                    color = Muted,
                    fontSize = 12.sp,
                )
                Text(
                    "Pixel placement matches the submitted map. Physical brightness depends on the phone and Nothing OS.",
                    color = Muted,
                    fontSize = 11.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
                if (vm.glyph.directDisplay)
                    Button(
                        onClick = vm::previewOnPhone,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                    ) {
                        Text("Test on phone · 5 seconds")
                    }
                else if (vm.glyph.supported)
                    Text(
                        "Use Custom Glyph in Always-on Glyph Toy to test this design.",
                        color = Muted,
                        fontSize = 12.sp,
                    )
                TextButton(
                    onClick = { preview = false },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                ) {
                    Text("Done")
                }
            }
        }
}
