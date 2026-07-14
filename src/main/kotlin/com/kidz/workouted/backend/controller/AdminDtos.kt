package com.kidz.workouted.backend.controller

import java.time.LocalDateTime

data class AdminMuscleRankDto(
    val muscleId: String,
    val rankName: String,
    val score: Double
)

data class AdminWorkoutSetDto(
    val id: Long,
    val exerciseId: Long,
    val weight: Double,
    val reps: Int
)

data class AdminWorkoutDto(
    val id: Long,
    val localId: Long,
    val timestamp: Long,
    val notes: String?,
    val setsCount: Int,
    val sets: List<AdminWorkoutSetDto>,
    val createdAt: LocalDateTime
)

data class AdminUserDetailsDto(
    val user: AdminUserDto,
    val ranks: List<AdminMuscleRankDto>,
    val workouts: List<AdminWorkoutDto>
)
