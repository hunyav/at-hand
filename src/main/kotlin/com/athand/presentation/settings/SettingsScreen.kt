package com.athand.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    themeLabel: String,
    dataDirectoryPath: String,
    reminderEnabled: Boolean,
    reminderTimeText: String,
    onReminderEnabledChange: (Boolean) -> Unit,
    onReminderTimeTextChange: (String) -> Unit,
    onSaveReminderClick: () -> Unit,
    isSavingReminder: Boolean,
    reminderStatusMessage: String?,
    reminderErrorMessage: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.titleLarge)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Theme", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = themeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Daily reminder", style = MaterialTheme.typography.titleSmall)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = reminderEnabled,
                        onCheckedChange = onReminderEnabledChange,
                        enabled = !isSavingReminder
                    )
                    Text(
                        text = "Enable reminder",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                OutlinedTextField(
                    value = reminderTimeText,
                    onValueChange = onReminderTimeTextChange,
                    enabled = !isSavingReminder,
                    singleLine = true,
                    label = { Text("Reminder time (HH:mm)") },
                    supportingText = { Text("Example: 09:30") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (reminderErrorMessage != null) {
                    Text(
                        text = reminderErrorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (reminderStatusMessage != null) {
                    Text(
                        text = reminderStatusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Button(
                    onClick = onSaveReminderClick,
                    enabled = !isSavingReminder
                ) {
                    Text(if (isSavingReminder) "Saving..." else "Save reminder")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Data location", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = dataDirectoryPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
