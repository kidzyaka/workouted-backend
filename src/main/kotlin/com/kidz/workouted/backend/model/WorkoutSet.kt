package com.kidz.workouted.backend.model

import jakarta.persistence.*

@Entity
@Table(name = "workout_sets")
data class WorkoutSet(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_id", nullable = false)
    val workout: Workout,

    @Column(name = "local_id", nullable = false)
    val localId: Long,

    @Column(name = "exercise_id", nullable = false)
    val exerciseId: Long,

    val weight: Double,
    val reps: Int
)
