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
    private val workoutRepository: WorkoutRepository
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

            LeaderboardEntry(
                friendId = u.id,
                username = u.username,
                friendCode = u.friendCode,
                muscleScores = muscleScores,
                recentWorkoutTimestamps = userWorkouts
            )
        }

        return ResponseEntity.ok(leaderboard)
    }
}
