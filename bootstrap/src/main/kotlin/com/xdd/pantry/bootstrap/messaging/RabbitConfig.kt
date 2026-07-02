package com.xdd.pantry.bootstrap.messaging

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.module.kotlin.jacksonMapperBuilder

@Configuration
class RabbitConfig {

    @Bean
    fun receiptsExchange() = DirectExchange(EXCHANGE)

    @Bean
    fun receiptsQueue(): Queue =
        QueueBuilder.durable(QUEUE).deadLetterExchange(DEAD_LETTER_EXCHANGE).deadLetterRoutingKey(ROUTING_KEY).build()

    @Bean
    fun receiptsBinding(): Binding =
        BindingBuilder.bind(receiptsQueue()).to(receiptsExchange()).with(ROUTING_KEY)

    @Bean
    fun receiptsDeadLetterExchange() = DirectExchange(DEAD_LETTER_EXCHANGE)

    @Bean
    fun receiptsDeadLetterQueue(): Queue = QueueBuilder.durable(DEAD_LETTER_QUEUE).build()

    @Bean
    fun receiptsDeadLetterBinding(): Binding =
        BindingBuilder.bind(receiptsDeadLetterQueue()).to(receiptsDeadLetterExchange()).with(ROUTING_KEY)

    @Bean
    fun rabbitMessageConverter(): MessageConverter =
        JacksonJsonMessageConverter(jacksonMapperBuilder().build())

    companion object {
        const val EXCHANGE = "pantry.receipts"
        const val QUEUE = "pantry.receipts.process"
        const val ROUTING_KEY = "receipt.submitted"
        const val DEAD_LETTER_EXCHANGE = "pantry.receipts.dlx"
        const val DEAD_LETTER_QUEUE = "pantry.receipts.dlq"
    }
}
