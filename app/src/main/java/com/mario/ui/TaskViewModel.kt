package com.mario.ui

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DevTaskUiState(
    val tasks: List<Task> = emptyList(),
    val categories: List<DevCategory> = DevCategory.entries
)

class DevViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(DevTaskUiState())
    val uiState: StateFlow<DevTaskUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { 
            it.copy(
                tasks = listOf(
                    Task(title = "Fix production bug", category = DevCategory.Bug, priority = Priority.Critical, scheduledTime = "09:00 AM"),
                    Task(title = "Team Sync", category = DevCategory.Review, priority = Priority.High, scheduledTime = "11:30 AM"),
                    Task(title = "Code Refactoring", category = DevCategory.Refactor, priority = Priority.Medium)
                )
            )
        }
    }

    fun addTask(title: String, category: DevCategory, priority: Priority, scheduledTime: String?, timerMinutes: Int? = null) {
        val newTask = Task(
            title = title, 
            category = category, 
            priority = priority,
            scheduledTime = scheduledTime,
            timerDurationMinutes = timerMinutes
        )
        
        _uiState.update { it.copy(tasks = it.tasks + newTask) }
        
        if (timerMinutes != null) {
            scheduleNotification(title, timerMinutes)
        }
    }

    private fun scheduleNotification(taskTitle: String, minutes: Int) {
        val context = getApplication<Application>().applicationContext
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra("TASK_TITLE", taskTitle)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context, 
            taskTitle.hashCode(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000)
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.e("DevViewModel", "Cannot schedule exact alarm", e)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun toggleTaskCompletion(taskId: String) {
        _uiState.update { state ->
            state.copy(
                tasks = state.tasks.map {
                    if (it.id == taskId) it.copy(isCompleted = !it.isCompleted) else it
                }
            )
        }
    }

    fun deleteTask(taskId: String) {
        _uiState.update { state ->
            state.copy(tasks = state.tasks.filter { it.id != taskId })
        }
    }
}
