package com.kidz.workouted.backend.controller

import com.kidz.workouted.backend.model.User
import com.kidz.workouted.backend.repository.FriendshipRepository
import com.kidz.workouted.backend.repository.UserMuscleRankRepository
import com.kidz.workouted.backend.repository.UserRepository
import com.kidz.workouted.backend.repository.WorkoutRepository
import com.kidz.workouted.backend.security.JwtUtil
import jakarta.transaction.Transactional
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userRepository: UserRepository,
    private val friendshipRepository: FriendshipRepository,
    private val workoutRepository: WorkoutRepository,
    private val userMuscleRankRepository: UserMuscleRankRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtUtil: JwtUtil
) {

    data class AuthRequest(val username: String, val password: String)
    data class AuthResponse(val token: String, val friendCode: String)

    @PostMapping("/register")
    fun register(@RequestBody request: AuthRequest): ResponseEntity<Any> {
        val cleanUsername = request.username.trim().lowercase()
        if (userRepository.findFirstByUsernameIgnoreCase(cleanUsername).isPresent) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Username already taken"))
        }

        // Generate a 6-character unique friend code
        var friendCode = ""
        var isUnique = false
        while (!isUnique) {
            friendCode = UUID.randomUUID().toString().substring(0, 6).uppercase()
            if (userRepository.findByFriendCode(friendCode).isEmpty) {
                isUnique = true
            }
        }

        val user = User(
            username = cleanUsername,
            passwordHash = passwordEncoder.encode(request.password)!!,
            friendCode = friendCode
        )
        userRepository.save(user)

        val token = jwtUtil.generateToken(user.username)
        return ResponseEntity.ok(AuthResponse(token, friendCode))
    }

    @PostMapping("/login")
    fun login(@RequestBody request: AuthRequest): ResponseEntity<Any> {
        val cleanUsername = request.username.trim().lowercase()
        val user = userRepository.findFirstByUsernameIgnoreCase(cleanUsername).orElse(null)
        
        if (user == null || !passwordEncoder.matches(request.password, user.passwordHash)) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Invalid credentials"))
        }

        val token = jwtUtil.generateToken(user.username)
        return ResponseEntity.ok(AuthResponse(token, user.friendCode))
    }

    @DeleteMapping("/account")
    @Transactional
    fun deleteAccount(): ResponseEntity<Any> {
        val authentication = SecurityContextHolder.getContext().authentication
        val currentUsername = authentication?.name
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "Not authenticated"))
        
        val user = userRepository.findFirstByUsernameIgnoreCase(currentUsername).orElse(null)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "User not found"))

        // Delete associated records
        val friendships = friendshipRepository.findByRequesterOrAddressee(user, user)
        friendshipRepository.deleteAll(friendships)
        
        val workouts = workoutRepository.findByUser(user)
        workoutRepository.deleteAll(workouts)
        
        val muscleRanks = userMuscleRankRepository.findByUser(user)
        userMuscleRankRepository.deleteAll(muscleRanks)
        
        // Delete user
        userRepository.delete(user)
        
        return ResponseEntity.ok(mapOf("message" to "Account deleted successfully"))
    }
}
