package com.kidz.workouted.backend.repository

import com.kidz.workouted.backend.model.User
import com.kidz.workouted.backend.model.UserMuscleRank
import org.springframework.data.jpa.repository.JpaRepository

interface UserMuscleRankRepository : JpaRepository<UserMuscleRank, Long> {
    fun findByUser(user: User): List<UserMuscleRank>
    fun findByUserAndMuscleId(user: User, muscleId: String): UserMuscleRank?
    fun findByUserIn(users: List<User>): List<UserMuscleRank>
}
