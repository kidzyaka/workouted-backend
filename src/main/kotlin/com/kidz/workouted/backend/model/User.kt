package com.kidz.workouted.backend.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "users")
data class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val username: String,

    @Column(name = "friend_code", unique = true, nullable = false, length = 6)
    var friendCode: String,

    @Column(nullable = false, name = "password_hash")
    val passwordHash: String,

    @Column(nullable = true)
    var height: Double? = null,

    @Column(nullable = true)
    var weight: Double? = null,

    @Column(nullable = true)
    var age: Int? = null,

    @Column(name = "default_color", nullable = true)
    var defaultColor: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
