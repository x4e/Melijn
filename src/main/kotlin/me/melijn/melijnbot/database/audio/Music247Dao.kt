package me.melijn.melijnbot.database.audio

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class Music247Dao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "music247"
    override val tableStructure: String = "guildId bigint"
    override val primaryKey: String = "guildId"

    override val cacheName: String = "music247"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun add(guildId: Long) {
        driverManager.executeUpdate(
            "INSERT INTO $table (guildId) VALUES (?) ON CONFLICT ($primaryKey) DO NOTHING",
            guildId
        )

    }

    suspend fun contains(guildId: Long): Boolean = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            it.resume(rs.next())
        }, guildId)
    }

    fun remove(guildId: Long) {
        driverManager.executeUpdate(
            "DELETE FROM $table WHERE guildId = ?",
            guildId
        )
    }
}