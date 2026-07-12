package com.kidz.workouted.backend.repository

import com.kidz.workouted.backend.model.User
import com.kidz.workouted.backend.model.Workout
import org.springframework.data.jpa.repository.JpaRepository

interface WorkoutRepository : JpaRepository<Workout, Long> {
    fun findByUser(user: User): List<Workout>
    fun findByUserAndLocalId(user: User, localId: Long): Workout?
    fun findTop3ByUserOrderByTimestampDesc(user: User): List<Workout>
    fun findTop3ByUserInOrderByTimestampDesc(users: List<User>): List<Workout>
}
