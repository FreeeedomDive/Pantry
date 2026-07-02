package com.xdd.pantry.bootstrap.telegram

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.telegram.telegrambots.meta.api.objects.message.Message
import kotlin.time.Duration

class TelegramAlbumBuffer(
    private val scope: CoroutineScope,
    private val window: Duration,
    private val onComplete: suspend (List<Message>) -> Unit,
) {
    private val mutex = Mutex()
    private val pending = mutableMapOf<String, MutableList<Message>>()
    private val flushes = mutableMapOf<String, Job>()

    suspend fun add(message: Message) {
        val groupId = message.mediaGroupId ?: return
        mutex.withLock {
            pending.getOrPut(groupId) { mutableListOf() }.add(message)
            flushes[groupId]?.cancel()
            flushes[groupId] = scope.launch {
                delay(window)
                flush(groupId)
            }
        }
    }

    private suspend fun flush(groupId: String) {
        val messages = mutex.withLock {
            flushes.remove(groupId)
            pending.remove(groupId)
        } ?: return
        onComplete(messages)
    }
}
