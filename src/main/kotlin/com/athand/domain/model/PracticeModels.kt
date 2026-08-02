package com.athand.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PracticeWeek(
    val id: String,
    val title: String,
    val principle: String,
    val overview: String,
    val dailyPractices: List<DailyPractice>,
    val closingReflectionPrompt: String? = null,
    val sourceNote: String? = null,
    val tags: Set<String> = emptySet()
)

@Serializable
data class DailyPractice(
    val dayIndex: Int,
    val heading: String,
    val instruction: String,
    val reflectionPrompt: String? = null
)
