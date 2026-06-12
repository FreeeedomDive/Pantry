package com.xdd.pantry.bootstrap

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.xdd.pantry"])
class PantryApplication

fun main(args: Array<String>) {
    runApplication<PantryApplication>(*args)
}
