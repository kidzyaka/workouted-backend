package com.kidz.workouted.backend.dto

data class LeaderboardEntry(
    val friendId: Long,
    val username: String,
    val friendCode: String,
    val height: Double?,
    val weight: Double?,
    val age: Int?,
    val defaultColor: String?,
    val totalScore: Double,
    val muscleScores: Map<String, MuscleScoreDto>,
    val recentWorkoutTimestamps: List<Long>
)

data class MuscleScoreDto(
    val score: Double,
    val rank: String
)
