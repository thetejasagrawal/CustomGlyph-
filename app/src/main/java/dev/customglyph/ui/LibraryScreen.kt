package dev.customglyph.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.customglyph.model.SavedDesign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun LibraryScreen(vm: EditorViewModel, onOpen: () -> Unit) {
    val designs by vm.library.collectAsStateWithLifecycle()
    val editor by vm.editor.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val shown =
        designs
            .filter { filter == 0 || (filter == 1 && !it.recent) || (filter == 2 && it.recent) }
            .sortedByDescending { it.createdAt }
    var deleting by remember { mutableStateOf<SavedDesign?>(null) }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(156.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                Modifier.padding(top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                DotText("GALLERY", Modifier.weight(1f), size = 36.sp)
                Text(
                    "${designs.count { !it.recent }} saved\n${designs.count { it.recent }} recent",
                    fontSize = 10.sp,
                    lineHeight = 17.sp,
                    color = Muted,
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SegmentControl(
                listOf("All", "Saved", "Recent"),
                filter,
                { filter = it },
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
        }
        item {
            Surface(
                onClick = onOpen,
                color = Paper,
                contentColor = Ink,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 232.dp),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(32.dp))
                    Column {
                        DotText("NEW\nGLYPH", size = 32.sp)
                        Text(
                            "Open editor",
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                }
            }
        }
        items(shown, key = { it.id }) { design ->
            val pixels by
                produceState<IntArray?>(null, design.frameFile) {
                    value =
                        withContext(Dispatchers.IO) {
                            runCatching {
                                    vm.repository.readSequence(design.frameFile).frames.first()
                                }
                                .getOrNull()
                        }
                }
            Surface(
                onClick = {
                    vm.openDesign(design)
                    onOpen()
                },
                enabled = !editor.busy,
                color = Panel,
                shape = RoundedCornerShape(28.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    MatrixPreview(
                        pixels ?: IntArray(design.draft.profile.size * design.draft.profile.size),
                        Modifier.fillMaxWidth(),
                        profile = design.draft.profile,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(start = 4.dp)) {
                            Text(
                                design.name,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                if (design.recent) "Recent edit" else "Saved design",
                                fontSize = 9.sp,
                                color = Muted,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        IconButton(
                            onClick = { deleting = design },
                            enabled = !editor.busy,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                "Delete ${design.name}",
                                Modifier.size(20.dp),
                                tint = Muted,
                            )
                        }
                    }
                }
            }
        }
        if (shown.isEmpty())
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    if (filter == 2) "Applied designs and replaced edits appear here."
                    else "Save a design from the editor to keep it here.",
                    color = Muted,
                    fontSize = 12.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(vertical = 14.dp, horizontal = 4.dp),
                )
            }
    }
    deleting?.let { design ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete design?") },
            text = { Text("${design.name} and its schedules will be removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteDesign(design.id)
                        deleting = null
                    }
                ) {
                    Text("Delete", color = Red)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}
