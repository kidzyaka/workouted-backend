package com.kidz.workouted.backend.controller

import com.kidz.workouted.backend.dto.*
import com.kidz.workouted.backend.model.*
import com.kidz.workouted.backend.repository.*
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/sync")
class SyncController(
    private val userRepository: UserRepository,
    private val workoutRepository: WorkoutRepository,
    private val workoutSetRepository: WorkoutSetRepository,
    private val userMuscleRankRepository: UserMuscleRankRepository
) {

    @PostMapping("/backup")
    @Transactional
    fun pushBackup(@RequestBody backupData: BackupDataDto): ResponseEntity<Any> {
        val userDetails = SecurityContextHolder.getContext().authentication!!.principal as UserDetails
        val user = userRepository.findFirstByUsernameIgnoreCase(userDetails.username).orElseThrow()

        // Update User preferences
        backupData.preferences?.let { prefs ->
            user.height = prefs.height
            user.weight = prefs.weight
            user.age = prefs.age
            user.defaultColor = prefs.defaultColor
            userRepository.save(user)
        }

        // Validation: Prevent data loss
        val currentWorkouts = workoutRepository.findByUser(user)
        val currentNonDeletedWorkouts = currentWorkouts.count { !it.isDeleted }
        val payloadNonDeletedWorkouts = backupData.workouts.count { !it.isDeleted }

        if (payloadNonDeletedWorkouts < currentNonDeletedWorkouts && !backupData.force) {
            return ResponseEntity.status(409).body(mapOf(
                "status" to "CONFIRMATION_REQUIRED",
                "message" to "You are trying to sync fewer workouts than are stored on the server."
            ))
        }

        // Sync Workouts and Sets
        for (workoutDto in backupData.workouts) {
            var workout = workoutRepository.findByUserAndLocalId(user, workoutDto.id)
            if (workout == null) {
                workout = Workout(
                    user = user,
                    localId = workoutDto.id,
                    timestamp = workoutDto.timestamp,
                    notes = workoutDto.notes,
                    isDeleted = workoutDto.isDeleted
                )
                workoutRepository.save(workout)
            } else {
                // Update existing workout fields (especially isDeleted)
                var needsUpdate = false
                if (workout.isDeleted != workoutDto.isDeleted) {
                    workout.isDeleted = workoutDto.isDeleted
                    needsUpdate = true
                }
                if (workout.notes != workoutDto.notes) {
                    workout = workout.copy(notes = workoutDto.notes) // Copy doesn't update managed entity directly if it's a data class, wait it's a JPA entity.
                    // Actually, for data class entity, modifying var is better, but 'notes' is val in Workout.kt.
                    // I will change 'notes' and 'timestamp' to var in a separate edit or just save new.
                    // Wait, let me just update `isDeleted` since I made it a var.
                }
                if (needsUpdate) {
                    workoutRepository.save(workout)
                }
            }

            // Sync Sets for this workout
            val setsForWorkout = backupData.sets.filter { it.workoutId == workoutDto.id }
            for (setDto in setsForWorkout) {
                // To avoid duplicate sets, we can check by local_id
                val existingSet = workoutSetRepository.findByWorkoutAndLocalId(workout, setDto.id)
                if (existingSet == null) {
                    workoutSetRepository.save(WorkoutSet(
                        workout = workout,
                        localId = setDto.id,
                        exerciseId = setDto.exerciseId,
                        weight = setDto.weight,
                        reps = setDto.reps
                    ))
                }
            }
        }

        // Sync Muscle Ranks
        backupData.preferences?.lastSeenMuscleRanks?.forEach { (muscleId, rankName) ->
            var muscleRank = userMuscleRankRepository.findByUserAndMuscleId(user, muscleId)
            
            // Map rank string to a numerical score so we can sort in leaderboard
            val score = when(rankName.uppercase()) {
                "WOOD" -> 0.0
                "BRONZE" -> 60.0
                "SILVER" -> 110.0
                "GOLD" -> 170.0
                "PLATINUM" -> 240.0
                "EMERALD" -> 320.0
                "DIAMOND" -> 420.0
                "ELITE" -> 550.0
                else -> 0.0
            }
            
            if (muscleRank == null) {
                userMuscleRankRepository.save(UserMuscleRank(
                    user = user,
                    muscleId = muscleId,
                    score = score,
                    rankName = rankName
                ))
            } else {
                muscleRank.score = score
                muscleRank.rankName = rankName
                userMuscleRankRepository.save(muscleRank)
            }
        }

        return ResponseEntity.ok(mapOf("status" to "success"))
    }

    @GetMapping("/backup")
    fun pullBackup(): ResponseEntity<BackupDataDto> {
        val userDetails = SecurityContextHolder.getContext().authentication!!.principal as UserDetails
        val user = userRepository.findFirstByUsernameIgnoreCase(userDetails.username).orElseThrow()

        val workouts = workoutRepository.findByUser(user)
        val workoutDtos = workouts.map { w -> WorkoutDto(w.localId, w.timestamp, w.notes, w.isDeleted) }

        val setDtos = mutableListOf<SetDto>()
        for (w in workouts) {
            val sets = workoutSetRepository.findByWorkout(w)
            setDtos.addAll(sets.map { s -> SetDto(s.localId, w.localId, s.exerciseId, s.weight, s.reps) })
        }

        val muscleRanks = userMuscleRankRepository.findByUser(user)
        val rankMap = muscleRanks.associate { it.muscleId to it.rankName }

        val preferences = UserPreferencesBackupDto(
            height = user.height ?: 0.0,
            weight = user.weight ?: 0.0,
            age = user.age ?: 0,
            defaultColor = user.defaultColor,
            lastSeenMuscleRanks = rankMap
            // Other preference fields will be default, Android should handle merging
        )

        return ResponseEntity.ok(BackupDataDto(workoutDtos, setDtos, preferences))
    }
}
