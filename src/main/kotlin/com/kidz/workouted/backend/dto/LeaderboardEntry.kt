package com.kidz.workouted.backend.dto

data class LeaderboardEntry(
    val friendId: Long,
    val username: String,
    val friendCode: String,
    val muscleScores: Map<String, MuscleScoreDto>,
    val recentWorkoutTimestamps: List<Long>
)

data class MuscleScoreDto(
    val score: Double,
    val rank: String
)
