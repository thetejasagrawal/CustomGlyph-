package dev.customglyph.ui

import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.customglyph.model.ScheduleRule
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
internal fun ScheduleScreen(vm: EditorViewModel) {
    val designs by vm.library.collectAsStateWithLifecycle()
    val rules by vm.rules.collectAsStateWithLifecycle()
    val editor by vm.editor.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(vm.repository.schedulesEnabled) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }
    var start by rememberSaveable { mutableIntStateOf(9 * 60) }
    var end by rememberSaveable { mutableIntStateOf(18 * 60) }
    var days by rememberSaveable { mutableStateOf((1..7).toList()) }
    var timerMinutes by rememberSaveable { mutableStateOf("30") }
    var stopAt by remember { mutableLongStateOf(vm.repository.stopAt) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val form = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(editingId) { if (editingId != null) form.bringIntoView() }
    val design = designs.firstOrNull { it.id == selected } ?: designs.firstOrNull()
    fun pickTime(current: Int, change: (Int) -> Unit) {
        TimePickerDialog(
                context,
                { _, hour, minute -> change(hour * 60 + minute) },
                current / 60,
                current % 60,
                DateFormat.is24HourFormat(context),
            )
            .show()
    }
    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DotText("SCHEDULE", size = 36.sp, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
        Surface(color = Panel, shape = RoundedCornerShape(28.dp)) {
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Schedule, null, Modifier.size(22.dp))
                Text("Use schedules", Modifier.weight(1f).padding(start = 12.dp), fontSize = 13.sp)
                Switch(
                    enabled,
                    {
                        enabled = it
                        vm.repository.schedulesEnabled = it
                    },
                )
            }
        }
        Text(
            "Outside these times, the last applied design is used. Later rules take priority when times overlap.",
            color = Muted,
            fontSize = 11.sp,
            lineHeight = 18.sp,
        )
        rules.forEach { rule ->
            Surface(color = Panel, shape = RoundedCornerShape(28.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            designs.firstOrNull { it.id == rule.designId }?.name
                                ?: "Unavailable design",
                            fontSize = 14.sp,
                        )
                        Text(
                            "${timeLabel(rule.startMinute)} – ${timeLabel(rule.endMinute)}",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Text(
                            rule.days.sorted().joinToString(" ") {
                                DayOfWeek.of(it)
                                    .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                            },
                            color = Muted,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    IconButton(
                        onClick = {
                            selected = rule.designId
                            start = rule.startMinute
                            end = rule.endMinute
                            days = rule.days.sorted()
                            editingId = rule.id
                            scope.launch { form.bringIntoView() }
                        },
                        enabled = !editor.busy,
                    ) {
                        Icon(Icons.Outlined.Edit, "Edit schedule", Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = {
                            vm.deleteRule(rule.id)
                            if (editingId == rule.id) editingId = null
                        },
                        enabled = !editor.busy,
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, "Delete schedule")
                    }
                }
            }
        }
        Surface(
            color = Panel,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.bringIntoViewRequester(form),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DotText(if (editingId == null) "NEW SCHEDULE" else "EDIT SCHEDULE", size = 25.sp)
                if (design == null)
                    Text("Save a design in the editor first.", color = Muted, fontSize = 12.sp)
                else {
                    Box {
                        OutlinedButton(
                            onClick = { menu = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(design.name, maxLines = 1)
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            designs.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text("${item.name} · ${item.draft.profile.title}") },
                                    onClick = {
                                        selected = item.id
                                        menu = false
                                    },
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TimeTile("FROM", start, Modifier.weight(1f)) {
                            pickTime(start) { start = it }
                        }
                        TimeTile("UNTIL", end, Modifier.weight(1f)) { pickTime(end) { end = it } }
                    }
                    Text("Start → end · local time", fontSize = 10.sp, color = Muted)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        (1..7).forEach { day ->
                            val label =
                                DayOfWeek.of(day)
                                    .getDisplayName(TextStyle.NARROW, Locale.getDefault())
                            Surface(
                                onClick = { days = if (day in days) days - day else days + day },
                                color = if (day in days) Paper else Ink,
                                contentColor = if (day in days) Ink else Muted,
                                shape = CircleShape,
                                modifier =
                                    Modifier.weight(1f).height(48.dp).semantics {
                                        contentDescription =
                                            DayOfWeek.of(day)
                                                .getDisplayName(TextStyle.FULL, Locale.getDefault())
                                    },
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(label, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    if (start == end)
                        Text(
                            "Equal start and end times mean all day.",
                            color = Muted,
                            fontSize = 10.sp,
                        )
                    Button(
                        onClick = {
                            vm.saveRule(
                                ScheduleRule(
                                    id = editingId ?: UUID.randomUUID().toString(),
                                    designId = design.id,
                                    startMinute = start,
                                    endMinute = end,
                                    days = days.toSet(),
                                )
                            )
                            editingId = null
                        },
                        enabled = days.isNotEmpty() && !editor.busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (editingId == null) "Add schedule" else "Update schedule")
                    }
                    if (editingId != null)
                        TextButton(
                            onClick = { editingId = null },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Cancel edit")
                        }
                }
            }
        }
        HorizontalDivider(color = Line)
        DotText("STOP TIMER", size = 28.sp, modifier = Modifier.padding(top = 12.dp))
        Text(
            "Turn off all designs after a set time. Apply again to resume.",
            color = Muted,
            fontSize = 11.sp,
            lineHeight = 18.sp,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                timerMinutes,
                { timerMinutes = it.filter(Char::isDigit).take(4) },
                label = { Text("Minutes") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    vm.setStopTimer(timerMinutes.toInt())
                    stopAt = vm.repository.stopAt
                },
                enabled = timerMinutes.toIntOrNull() in 1..1440 && vm.repository.enabled,
            ) {
                Text("Set")
            }
        }
        if (!vm.repository.enabled)
            Text("Apply a design before setting a timer.", color = Muted, fontSize = 11.sp)
        if (stopAt > System.currentTimeMillis()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Stops at ${DateFormat.getTimeFormat(context).format(java.util.Date(stopAt))}",
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        vm.setStopTimer(0)
                        stopAt = 0
                    }
                ) {
                    Text("Cancel timer")
                }
            }
        }
        Text(
            "Schedules and timers run while the display service is active. In deep sleep or Always-On mode, a change can wait until Nothing OS next wakes the toy. No exact alarms are requested.",
            color = Muted,
            fontSize = 11.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }
}

internal fun timeLabel(minute: Int): String =
    String.format(Locale.ROOT, "%02d:%02d", minute / 60, minute % 60)

@Composable
private fun TimeTile(label: String, minute: Int, modifier: Modifier, onClick: () -> Unit) {
    LightWidget(modifier) {
        Surface(onClick = onClick, color = Paper, contentColor = Ink) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 18.dp)) {
                Text(label, fontSize = 9.sp)
                DotText(timeLabel(minute), size = 32.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
