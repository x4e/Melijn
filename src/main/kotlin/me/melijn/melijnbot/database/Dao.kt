package me.melijn.melijnbot.database

import com.fasterxml.jackson.module.kotlin.readValue
import io.lettuce.core.SetArgs
import me.melijn.melijnbot.objectMapper
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

abstract class Dao(val driverManager: DriverManager) {

    abstract val table: String
    abstract val tableStructure: String
    abstract val primaryKey: String

    open val uniqueKey: String = ""

    fun clear() {
        driverManager.clear(table)
    }

    suspend fun getRowCount() = suspendCoroutine<Long> {
        driverManager.executeQuery("SELECT COUNT(*) FROM $table", { rs ->
            rs.next()
            it.resume(rs.getLong(1))
        })
    }
}

abstract class CacheDao(val driverManager: DriverManager) {

    abstract val cacheName: String

    fun setCacheEntry(key: Any, value: Any, ttlM: Int? = null) =
        driverManager.setCacheEntry("$cacheName:$key", value.toString(), ttlM)

    fun setCacheEntryWithArgs(key: Any, value: Any, args: SetArgs? = null) =
        driverManager.setCacheEntryWithArgs("$cacheName:$key", value.toString(), args)

    suspend fun getCacheEntry(key: Any, ttlM: Int? = null): String? =
        driverManager.getCacheEntry("$cacheName:$key", ttlM)

    fun removeCacheEntry(key: Any) =
        driverManager.removeCacheEntry("$cacheName:$key")
}

abstract class CacheDBDao(driverManager: DriverManager) : Dao(driverManager) {

    abstract val cacheName: String

    fun setCacheEntry(key: Any, value: Any, ttlM: Int? = null) {
        if (value is String || value is Int || value is Long || value is Double || value is Byte || value is Short || value is Short) {
            driverManager.setCacheEntry("$cacheName:$key", value.toString(), ttlM)
        } else {
            driverManager.setCacheEntry("$cacheName:$key", objectMapper.writeValueAsString(value), ttlM)
        }

    }

    fun setCacheEntryWithArgs(key: Any, value: Any, args: SetArgs? = null) =
        driverManager.setCacheEntryWithArgs("$cacheName:$key", value.toString(), args)

    suspend fun getCacheEntry(key: Any, ttlM: Int? = null): String? =
        driverManager.getCacheEntry("$cacheName:$key", ttlM)

    suspend fun getIntFromCache(key: Any, ttlM: Int? = null): Int? = getCacheEntry(key, ttlM)?.toIntOrNull()
    suspend fun getLongFromCache(key: Any, ttlM: Int? = null): Long? = getCacheEntry(key, ttlM)?.toLongOrNull()
    suspend fun getDoubleFromCache(key: Any, ttlM: Int? = null): Double? = getCacheEntry(key, ttlM)?.toDoubleOrNull()
    suspend fun getFloatFromCache(key: Any, ttlM: Int? = null): Float? = getCacheEntry(key, ttlM)?.toFloatOrNull()

    suspend fun <K> getKListFromCache(key: Any, ttlM: Int? = null): List<K>? {
        return getCacheEntry(key, ttlM)?.let {
            objectMapper.readValue<List<K>>(it)
        }
    }

    suspend inline fun <reified K> getValueFromCache(key: Any, ttlM: Int? = null): K? {
        return getCacheEntry(key, ttlM)?.let { objectMapper.readValue<K>(it) }
    }

    fun removeCacheEntry(key: Any) =
        driverManager.removeCacheEntry("$cacheName:$key")

}