package me.melijn.melijnbot.database.message

import me.melijn.melijnbot.internals.threading.TaskManager

class MessageHistoryWrapper(val taskManager: TaskManager, private val messageHistoryDao: MessageHistoryDao) {

    suspend fun getMessageById(messageId: Long): DaoMessage? {
        return messageHistoryDao.get(messageId)
    }

    suspend fun addMessage(daoMessage: DaoMessage) {
        messageHistoryDao.add(daoMessage)
    }

    suspend fun setMessage(daoMessage: DaoMessage) {
        messageHistoryDao.set(daoMessage)
    }

    suspend fun clearOldMessages() {
        messageHistoryDao.clearOldMessages()
    }
}