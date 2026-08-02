package com.athand.presentation.week

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class WeekDayView(
    val dayIndex: Int,
    val heading: String,
    val instruction: String,
    val isCompleted: Boolean,
    val hasReflection: Boolean,
    val isToday: Boolean,
    val isFuture: Boolean
)

@Composable
fun WeekScreen(
    weekTitle: String,
    principle: String,
    days: List<WeekDayView>,
    isLoading: Boolean,
    errorMessage: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = weekTitle, style = MaterialTheme.typography.titleMedium)
                Text(text = principle, style = MaterialTheme.typography.bodyMedium)
            }
        }

        when {
            isLoading -> {
                Text(text = "Loading current week...", style = MaterialTheme.typography.bodyMedium)
            }

            errorMessage != null -> {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            days.isEmpty() -> {
                Text(text = "No daily practices available.", style = MaterialTheme.typography.bodyMedium)
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(days, key = { it.dayIndex }) { day ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            val subduedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            val textColor = when {
                                day.isFuture -> subduedColor
                                else -> MaterialTheme.colorScheme.onSurface
                            }

                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = day.heading + if (day.isToday) " • Today" else "",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = textColor
                                )
                                Text(
                                    text = day.instruction,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textColor
                                )
                                Text(
                                    text = buildStatusLine(day),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildStatusLine(day: WeekDayView): String {
    val completion = if (day.isCompleted) "completed" else "not completed"
    val reflection = if (day.hasReflection) "reflection saved" else "no reflection"
    return "Status: $completion • $reflection"
}
