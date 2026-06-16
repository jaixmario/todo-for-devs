package com.mario.ui

import java.util.UUID

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val isExpired: Boolean = false,
    val category: DevCategory = DevCategory.Feature,
    val priority: Priority = Priority.Medium,
    val scheduledTime: String? = null, // e.g., "8:00 PM"
    val timerDurationMinutes: Int? = null,
    val scheduledTimeMillis: Long? = null
)

enum class DevCategory {
    Feature, Bug, Review, Docs, Refactor
}

enum class Priority {
    Low, Medium, High, Critical
}
