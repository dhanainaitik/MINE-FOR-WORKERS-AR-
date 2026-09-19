package com.minesafe.ar

import com.minesafe.ar.training.TrainingModule
import com.minesafe.ar.training.TrainingState
import com.minesafe.ar.training.TrainingViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TrainingViewModelTest {

    private lateinit var viewModel: TrainingViewModel

    @Before
    fun setUp() {
        viewModel = TrainingViewModel()
    }

    @Test
    fun testInitialState() {
        assertEquals(TrainingState.START, viewModel.currentState.value)
        assertEquals(100, viewModel.score.value)
        assertEquals(0, viewModel.mistakes.value)
        assertEquals(0, viewModel.stepsCompleted.value)
        assertEquals(TrainingModule.ELECTRICAL_FIRE, viewModel.selectedModule.value)
    }

    @Test
    fun testScoreDeductionAndFloorAtZero() {
        viewModel.registerMistake(5)
        assertEquals(95, viewModel.score.value)
        assertEquals(1, viewModel.mistakes.value)

        // Multiple mistakes
        repeat(25) {
            viewModel.registerMistake(5)
        }
        // Score should never drop below 0
        assertEquals(0, viewModel.score.value)
        assertTrue(viewModel.mistakes.value >= 26)
    }

    @Test
    fun testModuleSwitching() {
        viewModel.selectModule(TrainingModule.CHEMICAL_HAZARD)
        assertEquals(TrainingModule.CHEMICAL_HAZARD, viewModel.selectedModule.value)
        assertEquals(TrainingState.START, viewModel.currentState.value)
        assertEquals(100, viewModel.score.value)
    }

    @Test
    fun testElectricalFireStateProgression() {
        viewModel.selectModule(TrainingModule.ELECTRICAL_FIRE)
        viewModel.updateState(TrainingState.PLACE_DOORWAY)
        viewModel.updateState(TrainingState.ENTER_MINE)
        viewModel.updateState(TrainingState.FIRE_DETECTED)
        assertEquals(1, viewModel.stepsCompleted.value)

        viewModel.updateState(TrainingState.EXTINGUISHER_REACHED)
        assertEquals(2, viewModel.stepsCompleted.value)

        viewModel.updateState(TrainingState.OPEN_NOZZLE)
        assertEquals(3, viewModel.stepsCompleted.value)

        viewModel.updateState(TrainingState.AIM_AT_FIRE)
        assertEquals(4, viewModel.stepsCompleted.value)

        viewModel.updateState(TrainingState.DISCHARGE_EXTINGUISHER)
        assertEquals(5, viewModel.stepsCompleted.value)

        viewModel.updateState(TrainingState.FIRE_EXTINGUISHED)
        assertEquals(6, viewModel.stepsCompleted.value)
        assertEquals(viewModel.totalSteps, viewModel.stepsCompleted.value)
    }

    @Test
    fun testChemicalHazardStateProgression() {
        viewModel.selectModule(TrainingModule.CHEMICAL_HAZARD)
        viewModel.updateState(TrainingState.PLACE_DOORWAY)
        viewModel.updateState(TrainingState.ENTER_MINE)
        viewModel.updateState(TrainingState.CHEMICAL_HAZARD_DETECTED)
        assertEquals(1, viewModel.stepsCompleted.value)

        viewModel.updateState(TrainingState.MAINTAIN_SAFE_DISTANCE)
        assertEquals(2, viewModel.stepsCompleted.value)

        viewModel.updateState(TrainingState.LEAVE_HAZARD_ZONE)
        assertEquals(3, viewModel.stepsCompleted.value)

        viewModel.updateState(TrainingState.LOCATE_EMERGENCY_EQUIPMENT)
        assertEquals(4, viewModel.stepsCompleted.value)

        viewModel.updateState(TrainingState.PERFORM_SAFE_RESPONSE)
        assertEquals(5, viewModel.stepsCompleted.value)

        viewModel.updateState(TrainingState.HAZARD_CONTROLLED)
        assertEquals(6, viewModel.stepsCompleted.value)
        assertEquals(viewModel.totalSteps, viewModel.stepsCompleted.value)
    }

    @Test
    fun testResetTrainingRestoresDefaults() {
        viewModel.registerMistake(20)
        viewModel.updateState(TrainingState.FIRE_DETECTED)
        assertEquals(80, viewModel.score.value)

        viewModel.resetTraining()
        assertEquals(100, viewModel.score.value)
        assertEquals(0, viewModel.mistakes.value)
        assertEquals(0, viewModel.stepsCompleted.value)
        assertEquals(TrainingState.START, viewModel.currentState.value)
    }
}
