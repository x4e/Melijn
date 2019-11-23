package me.melijn.melijnbot.objects.services.bans

import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.commands.moderation.getUnbanMessage
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.database.ban.Ban
import me.melijn.melijnbot.database.ban.BanWrapper
import me.melijn.melijnbot.database.embed.EmbedDisabledWrapper
import me.melijn.melijnbot.database.logchannel.LogChannelWrapper
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.objects.services.Service
import me.melijn.melijnbot.objects.translation.getLanguage
import me.melijn.melijnbot.objects.utils.await
import me.melijn.melijnbot.objects.utils.sendEmbed
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class BanService(
    val shardManager: ShardManager,
    private val banWrapper: BanWrapper,
    private val logChannelWrapper: LogChannelWrapper,
    private val embedDisabledWrapper: EmbedDisabledWrapper,
    val daoManager: DaoManager
) : Service("ban") {

    private var scheduledFuture: ScheduledFuture<*>? = null

    private val banService = Runnable {
        runBlocking {
            val bans = banWrapper.getUnbannableBans()
            for (ban in bans) {
                val selfUser = shardManager.shards[0].selfUser
                val newBan = ban.run {
                    Ban(guildId, bannedId, banAuthorId, reason, selfUser.idLong, "Ban expired", startTime, endTime, false)
                }
                banWrapper.setBan(newBan)
                val guild = shardManager.getGuildById(ban.guildId) ?: continue

                //If ban exists, unban and send log messages
                val guildBan = guild.retrieveBanById(ban.bannedId).await()
                val bannedUser = shardManager.retrieveUserById(guildBan.user.idLong).await()
                guild.unban(bannedUser).queue()

                try {
                    val banAuthorId = ban.banAuthorId
                    val banAuthor = if (banAuthorId == null) {
                        null
                    } else {
                        shardManager.retrieveUserById(banAuthorId).await()
                    }
                    createAndSendUnbanMessage(guild, selfUser, bannedUser, banAuthor, newBan)
                } catch (t: Throwable) {
                    createAndSendUnbanMessage(guild, selfUser, bannedUser, null, newBan)
                }

            }
        }
    }

    //Sends unban message to tempban logchannel and the unbanned user
    private suspend fun createAndSendUnbanMessage(guild: Guild, unbanAuthor: User, bannedUser: User, banAuthor: User?, ban: Ban) {
        val language = getLanguage(daoManager, -1, guild.idLong)
        val msg = getUnbanMessage(language, guild, bannedUser, banAuthor, unbanAuthor, ban)
        val channelId = logChannelWrapper.logChannelCache.get(Pair(guild.idLong, LogChannelType.UNBAN)).await()
        val channel = guild.getTextChannelById(channelId)

        var success = false
        if (!bannedUser.isBot) {
            if (bannedUser.isFake) return
            try {
                val privateChannel = bannedUser.openPrivateChannel().await()
                sendEmbed(privateChannel, msg)
                success = true
            } catch (t: Throwable) {
            }
        }

        val msgLc = getUnbanMessage(language, guild, bannedUser, banAuthor, unbanAuthor, ban, true, bannedUser.isBot, success)

        if (channel == null && channelId != -1L) {
            logChannelWrapper.removeChannel(guild.idLong, LogChannelType.UNBAN)
            return
        } else if (channel == null) return

        sendEmbed(embedDisabledWrapper, channel, msgLc)
    }

    override fun start() {
        logger.info("Started BanService")
        scheduledFuture = scheduledExecutor.scheduleWithFixedDelay(banService, 1_000, 1_000, TimeUnit.MILLISECONDS)
    }

    override fun stop() {
        scheduledFuture?.cancel(false)
    }
}