package com.xdd.pantry.bootstrap.messaging

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

@Component
class ReceiptPublisher(private val rabbitTemplate: RabbitTemplate) {
    fun publish(receipt: ReceiptSubmitted) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, receipt)
    }
}
