package com.xdd.pantry.infrastructure

import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EntityScan
@EnableJpaRepositories
class PersistenceConfig
