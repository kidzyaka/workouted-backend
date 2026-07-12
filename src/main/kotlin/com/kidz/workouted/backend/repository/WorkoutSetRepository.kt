package com.kidz.workouted.backend.repository

import com.kidz.workouted.backend.model.Workout
import com.kidz.workouted.backend.model.WorkoutSet
import org.springframework.data.jpa.repository.JpaRepository

interface WorkoutSetRepository : JpaRepository<WorkoutSet, Long> {
    fun findByWorkout(workout: Workout): List<WorkoutSet>
    fun findByWorkoutAndLocalId(workout: Workout, localId: Long): WorkoutSet?
}
