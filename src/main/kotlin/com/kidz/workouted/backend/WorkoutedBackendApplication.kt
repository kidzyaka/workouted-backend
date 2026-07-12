package com.kidz.workouted.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WorkoutedBackendApplication

fun main(args: Array<String>) {
	runApplication<WorkoutedBackendApplication>(*args)
}
