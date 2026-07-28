package com.xdd.pantry.bootstrap.observability

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration(proxyBeanMethods = false)
@EnableScheduling
class ObservabilitySchedulingConfiguration
