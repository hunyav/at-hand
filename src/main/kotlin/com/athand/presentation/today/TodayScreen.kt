package com.athand.presentation.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.athand.presentation.theme.ThemeMode

@Composable
fun TodayScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onSettingsClick: () -> Unit,
    weekTitle: String,
    principle: String,
    todayHeading: String,
    todayInstruction: String,
    reflectionPrompt: String,
    reflectionText: String,
    onReflectionTextChange: (String) -> Unit,
    completed: Boolean,
    onCompletedChange: (Boolean) -> Unit,
    onSaveClick: () -> Unit,
    weekProgressLabel: String,
    isLoading: Boolean,
    isSaving: Boolean,
    statusMessage: String? = null,
    errorMessage: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "At Hand", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onThemeModeChange(themeMode) }) {
                    Text(text = "Theme: ${themeMode.label}")
                }
                IconButton(onClick = onSettingsClick) {
                    Text(text = "⚙")
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = weekTitle, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = principle,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Today", style = MaterialTheme.typography.titleMedium)
                Text(text = todayHeading, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = todayInstruction,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Text(
            text = weekProgressLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(
                checked = completed,
                onCheckedChange = onCompletedChange,
                enabled = !isLoading && !isSaving
            )
            Text(
                text = "Mark today's practice complete",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (errorMessage != null) {
            Text(text = errorMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        if (statusMessage != null) {
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        OutlinedTextField(
            value = reflectionText,
            onValueChange = onReflectionTextChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 6,
            enabled = !isLoading && !isSaving,
            label = { Text(reflectionPrompt) },
            supportingText = {
                if (isLoading) {
                    Text("Loading saved reflection...")
                } else {
                    Text("Saved locally when you press Save.")
                }
            }
        )

        Button(
            onClick = onSaveClick,
            enabled = !isLoading && !isSaving
        ) {
            Text(if (isSaving) "Saving..." else "Save")
        }
    }
}
