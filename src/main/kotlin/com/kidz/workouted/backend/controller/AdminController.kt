package com.kidz.workouted.backend.controller

import com.kidz.workouted.backend.repository.UserRepository
import com.kidz.workouted.backend.repository.WorkoutRepository
import com.kidz.workouted.backend.repository.UserMuscleRankRepository
import com.kidz.workouted.backend.repository.FriendshipRepository
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

data class AdminStatsResponse(
    val usersCount: Long,
    val workoutsCount: Long
)

data class AdminUserDto(
    val id: Long,
    val username: String,
    val friendCode: String,
    val height: Double?,
    val weight: Double?,
    val age: Int?,
    val defaultColor: String?,
    val createdAt: LocalDateTime
)

@RestController
@RequestMapping("/api/admin")
class AdminController(
    private val userRepository: UserRepository,
    private val workoutRepository: WorkoutRepository,
    private val userMuscleRankRepository: UserMuscleRankRepository,
    private val friendshipRepository: FriendshipRepository
) {

    @GetMapping("/stats")
    fun getStats(): ResponseEntity<AdminStatsResponse> {
        val stats = AdminStatsResponse(
            usersCount = userRepository.count(),
            workoutsCount = workoutRepository.count()
        )
        return ResponseEntity.ok(stats)
    }

    @GetMapping("/users")
    fun getUsers(): ResponseEntity<List<AdminUserDto>> {
        val users = userRepository.findAll().map { user ->
            AdminUserDto(
                id = user.id,
                username = user.username,
                friendCode = user.friendCode,
                height = user.height,
                weight = user.weight,
                age = user.age,
                defaultColor = user.defaultColor,
                createdAt = user.createdAt
            )
        }
        return ResponseEntity.ok(users)
    }

    @DeleteMapping("/users/{id}")
    @Transactional
    fun deleteUser(@PathVariable id: Long): ResponseEntity<Any> {
        return try {
            val user = userRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
            
            val ranks = userMuscleRankRepository.findByUser(user)
            userMuscleRankRepository.deleteAll(ranks)
            
            val workouts = workoutRepository.findByUser(user)
            workoutRepository.deleteAll(workouts)
            
            val friendships = friendshipRepository.findByRequesterOrAddressee(user, user)
            friendshipRepository.deleteAll(friendships)
            
            userRepository.deleteById(id)
            
            ResponseEntity.ok().build()
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity.badRequest().body(mapOf("error" to "Could not delete user: ${e.message}"))
        }
    }

    @GetMapping("/users/{id}")
    @Transactional(readOnly = true)
    fun getUserDetails(@PathVariable id: Long): ResponseEntity<AdminUserDetailsDto> {
        val user = userRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
        
        val userDto = AdminUserDto(
            id = user.id,
            username = user.username,
            friendCode = user.friendCode,
            height = user.height,
            weight = user.weight,
            age = user.age,
            defaultColor = user.defaultColor,
            createdAt = user.createdAt
        )
        
        val ranks = userMuscleRankRepository.findByUser(user).map { 
            AdminMuscleRankDto(it.muscleId, it.rankName, it.score) 
        }
        
        val workouts = workoutRepository.findByUser(user).sortedByDescending { it.timestamp }.map { w ->
            AdminWorkoutDto(
                id = w.id,
                localId = w.localId,
                timestamp = w.timestamp,
                notes = w.notes,
                setsCount = w.sets.size,
                sets = w.sets.map { s ->
                    AdminWorkoutSetDto(s.id, s.exerciseId, s.weight, s.reps)
                },
                createdAt = w.createdAt
            )
        }
        
        return ResponseEntity.ok(AdminUserDetailsDto(userDto, ranks, workouts))
    }

    @DeleteMapping("/workouts/{workoutId}")
    @Transactional
    fun deleteWorkout(@PathVariable workoutId: Long): ResponseEntity<Any> {
        return try {
            workoutRepository.deleteById(workoutId)
            ResponseEntity.ok().build()
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity.badRequest().body(mapOf("error" to "Could not delete workout: ${e.message}"))
        }
    }
}
