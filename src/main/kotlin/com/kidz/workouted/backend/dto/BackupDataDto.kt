package com.kidz.workouted.backend.dto

data class BackupDataDto(
    val workouts: List<WorkoutDto> = emptyList(),
    val sets: List<SetDto> = emptyList(),
    val preferences: UserPreferencesBackupDto? = null
)

data class WorkoutDto(
    val id: Long = 0,
    val timestamp: Long = 0,
    val notes: String? = null
)

data class SetDto(
    val id: Long = 0,
    val workoutId: Long = 0,
    val exerciseId: Long = 0,
    val weight: Double = 0.0,
    val reps: Int = 0
)

data class UserPreferencesBackupDto(
    val height: Double = 0.0,
    val weight: Double = 0.0,
    val age: Int = 0,
    val language: String = "",
    val isOnboardingCompleted: Boolean = false,
    val lastSeenMuscleRanks: Map<String, String> = emptyMap()
)
