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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    errorMessage: String? = null
) {
    var reflectionText by remember { mutableStateOf("") }

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

        if (errorMessage != null) {
            Text(text = errorMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        OutlinedTextField(
            value = reflectionText,
            onValueChange = { reflectionText = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 6,
            label = { Text(reflectionPrompt) }
        )
    }
}
