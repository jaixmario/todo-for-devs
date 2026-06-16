package com.mario.ui

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
                    Task(title = "Team Sync", category = DevCategory.Review, priority = Priority.High, scheduledTime = "11:30 AM")
                )
            )
        }
        
        // Background check for expired tasks
        viewModelScope.launch {
            while (true) {
                checkExpiredTasks()
                delay(60000) // Check every minute
            }
        }
    }

    private fun checkExpiredTasks() {
        val now = System.currentTimeMillis()
        _uiState.update { state ->
            state.copy(
                tasks = state.tasks.map { task ->
                    if (!task.isCompleted && !task.isExpired && task.scheduledTimeMillis != null && task.scheduledTimeMillis < now) {
                        task.copy(isExpired = true)
                    } else {
                        task
                    }
                }
            )
        }
    }

    fun addTask(title: String, category: DevCategory, priority: Priority, scheduledTimeStr: String?, timerMinutes: Int? = null) {
        var scheduledMillis: Long? = null
        
        if (scheduledTimeStr != null) {
            scheduledMillis = parseTimeToMillis(scheduledTimeStr)
        } else if (timerMinutes != null) {
            scheduledMillis = System.currentTimeMillis() + (timerMinutes * 60 * 1000)
        }

        val newTask = Task(
            title = title, 
            category = category, 
            priority = priority,
            scheduledTime = scheduledTimeStr,
            timerDurationMinutes = timerMinutes,
            scheduledTimeMillis = scheduledMillis
        )
        
        _uiState.update { it.copy(tasks = it.tasks + newTask) }
        
        if (scheduledMillis != null) {
            scheduleNotification(title, scheduledMillis)
        }
    }

    private fun parseTimeToMillis(timeStr: String): Long? {
        return try {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            val date = sdf.parse(timeStr) ?: return null
            val calendar = Calendar.getInstance()
            val timeCalendar = Calendar.getInstance().apply { time = date }
            
            calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
            calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            
            // If time has already passed today, schedule for tomorrow
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            calendar.timeInMillis
        } catch (e: Exception) {
            Log.e("DevViewModel", "Failed to parse time: $timeStr", e)
            null
        }
    }

    private fun scheduleNotification(taskTitle: String, triggerTime: Long) {
        val context = getApplication<Application>().applicationContext
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra("TASK_TITLE", taskTitle)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context, 
            taskTitle.hashCode() + triggerTime.toInt(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
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
                    if (it.id == taskId) it.copy(isCompleted = !it.isCompleted, isExpired = false) else it
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
