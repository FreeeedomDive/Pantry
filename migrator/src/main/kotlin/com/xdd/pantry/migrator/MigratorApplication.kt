package com.xdd.pantry.migrator

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import kotlin.system.exitProcess

@SpringBootApplication
class MigratorApplication

fun main(args: Array<String>) {
    exitProcess(SpringApplication.exit(SpringApplication.run(MigratorApplication::class.java, *args)))
}
