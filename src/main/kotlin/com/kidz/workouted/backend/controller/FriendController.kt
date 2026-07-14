package com.kidz.workouted.backend.controller

import com.kidz.workouted.backend.dto.*
import com.kidz.workouted.backend.model.*
import com.kidz.workouted.backend.repository.*
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/friends")
class FriendController(
    private val userRepository: UserRepository,
    private val friendshipRepository: FriendshipRepository,
    private val userMuscleRankRepository: UserMuscleRankRepository,
    private val workoutRepository: WorkoutRepository,
    private val workoutSetRepository: WorkoutSetRepository
) {

    private fun getCurrentUser(): User {
        val userDetails = SecurityContextHolder.getContext().authentication!!.principal as UserDetails
        return userRepository.findByUsername(userDetails.username).orElseThrow()
    }

    @PostMapping("/request")
    fun sendRequest(@RequestParam code: String): ResponseEntity<Any> {
        val user = getCurrentUser()
        val friend = userRepository.findByFriendCode(code).orElse(null)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "User with code $code not found"))

        if (user.id == friend.id) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Cannot add yourself"))
        }

        val existing = friendshipRepository.findByRequesterAndAddressee(user, friend) 
            ?: friendshipRepository.findByRequesterAndAddressee(friend, user)

        if (existing != null) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Friendship request already exists or accepted"))
        }

        val friendship = Friendship(requester = user, addressee = friend, status = FriendshipStatus.PENDING)
        friendshipRepository.save(friendship)

        return ResponseEntity.ok(mapOf("status" to "Request sent"))
    }

    @PostMapping("/accept")
    fun acceptRequest(@RequestParam friendshipId: Long): ResponseEntity<Any> {
        val user = getCurrentUser()
        val friendship = friendshipRepository.findById(friendshipId).orElse(null)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "Friendship request not found"))

        if (friendship.addressee.id != user.id) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Unauthorized to accept this request"))
        }

        friendship.status = FriendshipStatus.ACCEPTED
        friendshipRepository.save(friendship)

        return ResponseEntity.ok(mapOf("status" to "Request accepted"))
    }

    @GetMapping("/requests")
    fun getRequests(): ResponseEntity<List<Map<String, Any>>> {
        val user = getCurrentUser()
        val requests = friendshipRepository.findByAddresseeAndStatus(user, FriendshipStatus.PENDING)
        
        val result = requests.map {
            mapOf(
                "friendshipId" to it.id,
                "requesterUsername" to it.requester.username,
                "requesterCode" to it.requester.friendCode
            )
        }
        return ResponseEntity.ok(result)
    }

    @GetMapping("/leaderboard")
    fun getLeaderboard(): ResponseEntity<List<LeaderboardEntry>> {
        val user = getCurrentUser()
        
        // Find all accepted friends
        val friendships = friendshipRepository.findByRequesterAndStatusOrAddresseeAndStatus(
            user, FriendshipStatus.ACCEPTED, user, FriendshipStatus.ACCEPTED
        )
        
        val friendUsers = friendships.map { 
            if (it.requester.id == user.id) it.addressee else it.requester 
        }

        // Add the current user to the leaderboard too, to compare directly
        val allUsers = mutableListOf(user).apply { addAll(friendUsers) }

        val ranks = userMuscleRankRepository.findByUserIn(allUsers)
        val recentWorkouts = workoutRepository.findTop3ByUserInOrderByTimestampDesc(allUsers)

        val leaderboard = allUsers.map { u ->
            val userRanks = ranks.filter { it.user.id == u.id }
            val muscleScores = userRanks.associate { it.muscleId to MuscleScoreDto(it.score, it.rankName) }
            
            val userWorkouts = recentWorkouts.filter { it.user.id == u.id }.map { it.timestamp }

            val totalScore = userRanks.sumOf { it.score }

            LeaderboardEntry(
                friendId = u.id,
                username = u.username,
                friendCode = u.friendCode,
                height = u.height,
                weight = u.weight,
                age = u.age,
                defaultColor = u.defaultColor,
                totalScore = totalScore,
                muscleScores = muscleScores,
                recentWorkoutTimestamps = userWorkouts
            )
        }

        return ResponseEntity.ok(leaderboard)
    }

    @GetMapping("/stats/1rm")
    fun get1RmStats(@RequestParam exerciseId: Long): ResponseEntity<Map<String, List<OneRepMaxPointDto>>> {
        val user = getCurrentUser()
        
        val friendships = friendshipRepository.findByRequesterAndStatusOrAddresseeAndStatus(
            user, FriendshipStatus.ACCEPTED, user, FriendshipStatus.ACCEPTED
        )
        
        val friendUsers = friendships.map { 
            if (it.requester.id == user.id) it.addressee else it.requester 
        }

        // We also want to include the current user to potentially plot themselves? Or the client has its own data.
        // Let's just return for friends. The client has local data for the user.
        // Wait, the client can plot itself using local data. Returning friends is enough.
        // However, we need to calculate 1RM. For a given workout, 1RM = max(calculateOneRepMax(weight, reps)).
        // Since the backend doesn't have the exact formula if it's complex, we can use the standard Brzycki formula: 
        // weight * (36 / (37 - reps))
        // Or Epley: weight * (1 + reps / 30.0)
        // Let's use Epley as a generic fallback since the client does it locally. Or we can just calculate it here.

        val result = mutableMapOf<String, List<OneRepMaxPointDto>>()

        for (friend in friendUsers) {
            val workouts = workoutRepository.findByUser(friend)
            val workoutMap = workouts.associateBy { it.id }
            
            // Get all sets for the friend
            val allSets = mutableListOf<WorkoutSet>()
            for (w in workouts) {
                allSets.addAll(workoutSetRepository.findByWorkout(w))
            }

            // Filter sets by the requested exerciseId
            val exerciseSets = allSets.filter { it.exerciseId == exerciseId }

            val progressData = exerciseSets.groupBy { it.workout.id }
                .mapNotNull { (workoutId, sets) ->
                    val workout = workoutMap[workoutId] ?: return@mapNotNull null
                    val max1RM = sets.maxOf { set ->
                        val weight = set.weight
                        val reps = set.reps
                        if (reps <= 0) weight
                        else if (reps == 1) weight
                        else weight * (1.0 + reps / 30.0)
                    }
                    workout.timestamp to max1RM
                }
                .sortedBy { it.first }
                .map { (timestamp, oneRm) ->
                    OneRepMaxPointDto(timestamp, oneRm)
                }

            if (progressData.isNotEmpty()) {
                result[friend.id.toString()] = progressData
            }
        }
        return ResponseEntity.ok(result)
    }

    @GetMapping("/stats/volume")
    fun getVolumeStats(): ResponseEntity<Map<String, List<OneRepMaxPointDto>>> {
        val user = getCurrentUser()
        
        val friendships = friendshipRepository.findByRequesterAndStatusOrAddresseeAndStatus(
            user, FriendshipStatus.ACCEPTED, user, FriendshipStatus.ACCEPTED
        )
        
        val friendUsers = friendships.map { 
            if (it.requester.id == user.id) it.addressee else it.requester 
        }

        val result = mutableMapOf<String, List<OneRepMaxPointDto>>()

        for (friend in friendUsers) {
            val workouts = workoutRepository.findByUser(friend)
            val workoutMap = workouts.associateBy { it.id }
            
            val allSets = mutableListOf<WorkoutSet>()
            for (w in workouts) {
                allSets.addAll(workoutSetRepository.findByWorkout(w))
            }

            val progressData = allSets.groupBy { it.workout.id }
                .mapNotNull { (workoutId, sets) ->
                    val workout = workoutMap[workoutId] ?: return@mapNotNull null
                    val totalVolume = sets.sumOf { set ->
                        val weightToUse = if (set.weight > 0.0) set.weight else 1.0
                        weightToUse * set.reps
                    }
                    workout.timestamp to totalVolume
                }
                .sortedBy { it.first }
                .map { (timestamp, vol) ->
                    OneRepMaxPointDto(timestamp, vol)
                }

            if (progressData.isNotEmpty()) {
                result[friend.id.toString()] = progressData
            }
        }
        return ResponseEntity.ok(result)
    }
}
