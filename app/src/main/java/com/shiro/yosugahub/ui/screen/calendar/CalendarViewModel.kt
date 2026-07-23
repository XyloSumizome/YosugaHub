package com.shiro.yosugahub.ui.screen.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shiro.yosugahub.YosugaHubApplication
import com.shiro.yosugahub.data.calendar.CalendarSyncResult
import com.shiro.yosugahub.data.repository.CalendarRepository
import com.shiro.yosugahub.domain.model.CalendarEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** カレンダー画面が監視するUI状態。 */
data class CalendarUiState(
    val today: String = "",
    val todayEvents: List<CalendarEvent> = emptyList(),
    val upcomingEvents: List<CalendarEvent> = emptyList(),
    val pastEvents: List<CalendarEvent> = emptyList(),
    val isSyncing: Boolean = false,
)

class CalendarViewModel(
    private val calendarRepository: CalendarRepository,
) : ViewModel() {

    private val syncing = MutableStateFlow(false)

    fun hasPermission(): Boolean = calendarRepository.hasCalendarPermission()

    /** 端末カレンダーから読み直す。結果は UI へ返す。 */
    fun sync(onResult: (CalendarSyncResult) -> Unit) {
        viewModelScope.launch {
            syncing.value = true
            try {
                onResult(calendarRepository.sync())
            } finally {
                syncing.value = false
            }
        }
    }

    val uiState: StateFlow<CalendarUiState> = combine(
        calendarRepository.todayEvents(),
        calendarRepository.upcomingEvents(),
        calendarRepository.pastEvents(),
        syncing,
    ) { todayEvents, upcomingEvents, pastEvents, isSyncing ->
        CalendarUiState(
            today = calendarRepository.today,
            todayEvents = todayEvents,
            upcomingEvents = upcomingEvents,
            pastEvents = pastEvents,
            isSyncing = isSyncing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalendarUiState(),
    )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as YosugaHubApplication
                CalendarViewModel(app.container.calendarRepository)
            }
        }
    }
}
