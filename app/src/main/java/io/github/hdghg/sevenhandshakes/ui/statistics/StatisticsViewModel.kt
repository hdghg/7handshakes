package io.github.hdghg.sevenhandshakes.ui.statistics

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

/**
 * UiState for the statistics screen.
 */
data class StatisticsUiState(
    val isEmpty: Boolean = false,
    val isLoading: Boolean = false,
    val activeTasksPercent: Float = 0f,
    val completedTasksPercent: Float = 0f,
)

/**
 * ViewModel for the statistics screen.
 */
@HiltViewModel
class StatisticsViewModel
    @Inject
    constructor() : ViewModel() {
        val uiState: MutableStateFlow<StatisticsUiState> = MutableStateFlow(StatisticsUiState())

        fun refresh() {
        }
    }
