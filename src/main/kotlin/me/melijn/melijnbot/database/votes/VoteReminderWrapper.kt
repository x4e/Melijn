package me.melijn.melijnbot.database.votes

class VoteReminderWrapper(private val voteReminderDao: VoteReminderDao) {

    fun addReminder(userId: Long, remindAt: Long) {
        voteReminderDao.addReminder(userId, remindAt)
    }

    suspend fun getReminders(beforeMillis: Long): List<Long> {
        return voteReminderDao.getReminders(beforeMillis)
    }

    fun removeReminders(beforeMillis: Long) {
        voteReminderDao.removeReminders(beforeMillis)
    }
}