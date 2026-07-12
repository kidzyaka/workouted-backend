package com.kidz.workouted.backend.repository

import com.kidz.workouted.backend.model.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserRepository : JpaRepository<User, Long> {
    fun findByUsername(username: String): Optional<User>
    fun findByFriendCode(friendCode: String): Optional<User>
}
