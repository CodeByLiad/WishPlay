package com.jinatra.wishplay.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleaseDateField(value: String, onValueChange: (String) -> Unit,
                     label: String = "Release date", enabled: Boolean = true) {
    var showCalendar by rememberSaveable { mutableStateOf(false) }
    val date = runCatching { LocalDate.parse(value) }.getOrNull()
    val invalid = value.isNotBlank() && date == null
    OutlinedTextField(
        value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(),
        enabled = enabled, singleLine = true, label = { Text(label) },
        placeholder = { Text("YYYY-MM-DD") }, isError = invalid,
        supportingText = { Text(if (invalid) "Use YYYY-MM-DD or choose a date from the calendar."
            else "Leave empty if the date hasn't been announced.") },
        trailingIcon = {
            IconButton(onClick = { showCalendar = true }, enabled = enabled) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Choose $label")
            }
        }
    )
    if (showCalendar) {
        val picker = rememberDatePickerState(initialSelectedDateMillis =
            date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli())
        DatePickerDialog(onDismissRequest = { showCalendar = false },
            confirmButton = {
                TextButton(enabled = picker.selectedDateMillis != null, onClick = {
                    picker.selectedDateMillis?.let {
                        onValueChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString())
                    }
                    showCalendar = false
                }) { Text("Use date") }
            }, dismissButton = { TextButton(onClick = { showCalendar = false }) { Text("Cancel") } }
        ) { DatePicker(state = picker) }
    }
}
