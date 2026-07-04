package com.xdd.pantry.bootstrap.messaging

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

@Component
class ReceiptPublisher(private val rabbitTemplate: RabbitTemplate) {
    fun publishSubmitted(receipt: ReceiptSubmitted) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, receipt)
    }

    fun publishExtracted(extracted: ReceiptExtracted) {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.MATCH_ROUTING_KEY, extracted)
    }
}
