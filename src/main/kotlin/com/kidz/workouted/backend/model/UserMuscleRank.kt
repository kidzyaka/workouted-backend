package com.kidz.workouted.backend.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_muscle_ranks",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "muscle_id"])]
)
data class UserMuscleRank(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "muscle_id", nullable = false)
    val muscleId: String,

    @Column(nullable = false)
    var score: Double,

    @Column(name = "rank_name", nullable = false)
    var rankName: String,

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
