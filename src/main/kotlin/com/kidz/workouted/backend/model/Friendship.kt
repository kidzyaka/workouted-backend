package com.kidz.workouted.backend.model

import jakarta.persistence.*
import java.time.LocalDateTime

enum class FriendshipStatus {
    PENDING, ACCEPTED
}

@Entity
@Table(name = "friendships")
data class Friendship(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    val requester: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addressee_id", nullable = false)
    val addressee: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: FriendshipStatus = FriendshipStatus.PENDING,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
