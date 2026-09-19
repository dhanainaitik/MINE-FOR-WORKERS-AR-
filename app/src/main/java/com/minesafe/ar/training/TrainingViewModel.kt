package com.minesafe.ar.training

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TrainingViewModel(
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : ViewModel() {
    private val _selectedModule = MutableStateFlow(TrainingModule.ELECTRICAL_FIRE)
    val selectedModule: StateFlow<TrainingModule> = _selectedModule.asStateFlow()

    private val _currentState = MutableStateFlow(TrainingState.START)
    val currentState: StateFlow<TrainingState> = _currentState.asStateFlow()

    private val _score = MutableStateFlow(100)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _timeSeconds = MutableStateFlow(0)
    val timeSeconds: StateFlow<Int> = _timeSeconds.asStateFlow()

    private val _mistakes = MutableStateFlow(0)
    val mistakes: StateFlow<Int> = _mistakes.asStateFlow()

    private val _stepsCompleted = MutableStateFlow(0)
    val stepsCompleted: StateFlow<Int> = _stepsCompleted.asStateFlow()

    private val _distanceToObjective = MutableStateFlow(-1.0f)
    val distanceToObjective: StateFlow<Float> = _distanceToObjective.asStateFlow()

    private val _isSoundMuted = MutableStateFlow(false)
    val isSoundMuted: StateFlow<Boolean> = _isSoundMuted.asStateFlow()

    private val _nozzleReminderTrigger = MutableStateFlow(0)
    val nozzleReminderTrigger: StateFlow<Int> = _nozzleReminderTrigger.asStateFlow()

    private var timerJob: Job? = null
    private var reminderJob: Job? = null

    val totalSteps: Int
        get() = 6

    fun selectModule(module: TrainingModule) {
        _selectedModule.value = module
        resetTraining()
    }

    fun toggleSound() {
        _isSoundMuted.value = !_isSoundMuted.value
    }

    fun setSoundMuted(muted: Boolean) {
        _isSoundMuted.value = muted
    }

    fun updateDistanceToObjective(distance: Float) {
        _distanceToObjective.value = distance
    }

    fun updateState(newState: TrainingState) {
        if (_currentState.value != newState) {
            _currentState.value = newState

            // Cancel any pending nozzle reminder if we advanced past EXTINGUISHER_REACHED
            if (newState != TrainingState.EXTINGUISHER_REACHED) {
                stopNozzleReminder()
            }

            when (newState) {
                TrainingState.ENTER_MINE -> {
                    startTimer()
                }

                // Module 1: Electrical Fire steps
                TrainingState.FIRE_DETECTED -> {
                    _stepsCompleted.value = 1
                }
                TrainingState.EXTINGUISHER_REACHED -> {
                    _stepsCompleted.value = 2
                    startNozzleReminder()
                }
                TrainingState.OPEN_NOZZLE -> {
                    _stepsCompleted.value = 3
                }
                TrainingState.AIM_AT_FIRE -> {
                    _stepsCompleted.value = 4
                }
                TrainingState.DISCHARGE_EXTINGUISHER -> {
                    _stepsCompleted.value = 5
                }
                TrainingState.FIRE_EXTINGUISHED -> {
                    _stepsCompleted.value = 6
                    stopTimer()
                }

                // Module 2: Chemical Hazard steps
                TrainingState.CHEMICAL_HAZARD_DETECTED -> {
                    _stepsCompleted.value = 1
                }
                TrainingState.MAINTAIN_SAFE_DISTANCE -> {
                    _stepsCompleted.value = 2
                }
                TrainingState.LEAVE_HAZARD_ZONE -> {
                    _stepsCompleted.value = 3
                }
                TrainingState.LOCATE_EMERGENCY_EQUIPMENT -> {
                    _stepsCompleted.value = 4
                }
                TrainingState.PERFORM_SAFE_RESPONSE -> {
                    _stepsCompleted.value = 5
                }
                TrainingState.HAZARD_CONTROLLED -> {
                    _stepsCompleted.value = 6
                    stopTimer()
                }

                TrainingState.TRAINING_COMPLETE -> {
                    stopTimer()
                    stopNozzleReminder()
                }

                TrainingState.START -> {
                    resetTraining()
                }
                else -> {}
            }
        }
    }

    fun registerMistake(penalty: Int = 5) {
        _mistakes.value += 1
        deductScore(penalty)
    }

    private fun deductScore(points: Int) {
        val current = _score.value
        _score.value = (current - points).coerceAtLeast(0)
    }

    private fun startTimer() {
        if (timerJob == null) {
            timerJob = coroutineScope.launch {
                while (isActive) {
                    delay(1000)
                    _timeSeconds.value += 1
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun startNozzleReminder() {
        stopNozzleReminder()
        reminderJob = coroutineScope.launch {
            while (isActive) {
                delay(3500)
                if (_currentState.value == TrainingState.EXTINGUISHER_REACHED) {
                    _nozzleReminderTrigger.value += 1
                } else {
                    break
                }
            }
        }
    }

    private fun stopNozzleReminder() {
        reminderJob?.cancel()
        reminderJob = null
    }

    fun resetTraining() {
        stopTimer()
        stopNozzleReminder()
        _currentState.value = TrainingState.START
        _score.value = 100
        _timeSeconds.value = 0
        _mistakes.value = 0
        _stepsCompleted.value = 0
        _distanceToObjective.value = -1.0f
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
        stopNozzleReminder()
        coroutineScope.cancel()
    }
}
