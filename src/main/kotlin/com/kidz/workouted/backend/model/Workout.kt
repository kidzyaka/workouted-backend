package com.kidz.workouted.backend.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "workouts",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "local_id"])]
)
data class Workout(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "local_id", nullable = false)
    val localId: Long,

    @Column(nullable = false)
    val timestamp: Long,

    val notes: String? = null,

    @OneToMany(mappedBy = "workout", cascade = [CascadeType.ALL], orphanRemoval = true)
    var sets: MutableList<WorkoutSet> = mutableListOf(),

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false
)
