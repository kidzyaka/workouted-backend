package com.kidz.workouted.backend.controller

import com.kidz.workouted.backend.security.JwtUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class AdminLoginRequest(val password: String)
data class AdminLoginResponse(val token: String)

@RestController
@RequestMapping("/api/admin")
class AdminAuthController(
    private val jwtUtil: JwtUtil
) {
    @Value("\${app.admin.password}")
    private lateinit var adminPassword: String

    @PostMapping("/login")
    fun login(@RequestBody request: AdminLoginRequest): ResponseEntity<Any> {
        if (request.password == adminPassword) {
            val token = jwtUtil.generateToken("ADMIN_SYSTEM_USER")
            return ResponseEntity.ok(AdminLoginResponse(token))
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("error" to "Invalid admin password"))
    }
}
