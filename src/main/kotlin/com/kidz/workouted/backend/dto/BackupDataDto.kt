package com.kidz.workouted.backend.dto

data class BackupDataDto(
    val workouts: List<WorkoutDto> = emptyList(),
    val sets: List<SetDto> = emptyList(),
    val preferences: UserPreferencesBackupDto? = null,
    val force: Boolean = false
)

data class WorkoutDto(
    val id: Long = 0,
    val timestamp: Long = 0,
    val notes: String? = null,
    val isDeleted: Boolean = false
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
    val defaultColor: String? = null,
    val lastSeenMuscleRanks: Map<String, String> = emptyMap()
)
