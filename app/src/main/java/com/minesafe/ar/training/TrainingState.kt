package com.minesafe.ar.training

enum class TrainingModule {
    ELECTRICAL_FIRE,
    CHEMICAL_HAZARD
}

enum class TrainingState {
    START,
    SCAN_FLOOR,
    PLACE_DOORWAY,
    ENTER_MINE,

    // Module 1: Electrical Fire Safety (Sequential PASS Protocol)
    FIRE_DETECTED,
    GO_TO_EXTINGUISHER,
    EXTINGUISHER_REACHED,
    EXTINGUISHER_HELD,
    PULL_SAFETY_PIN,
    AIM_AT_FIRE_BASE,
    SQUEEZE_LEVER,
    SWEEP_SIDE_TO_SIDE,
    OPEN_NOZZLE,
    AIM_AT_FIRE,
    DISCHARGE_EXTINGUISHER,
    FIRE_EXTINGUISHED,

    // Module 2: Chemical Hazard Response
    CHEMICAL_HAZARD_DETECTED,
    MAINTAIN_SAFE_DISTANCE,
    LEAVE_HAZARD_ZONE,
    LOCATE_EMERGENCY_EQUIPMENT,
    PERFORM_SAFE_RESPONSE,
    HAZARD_CONTROLLED,

    // Universal Complete
    TRAINING_COMPLETE
}
