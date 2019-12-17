package me.melijn.melijnbot.objects.events.eventlisteners

import kotlinx.coroutines.async
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.database.message.DaoMessage
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.objects.events.AbstractListener
import me.melijn.melijnbot.objects.events.eventutil.FilterUtil
import me.melijn.melijnbot.objects.translation.*
import me.melijn.melijnbot.objects.utils.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent
import java.awt.Color

class MessageUpdateListener(container: Container) : AbstractListener(container) {

    override fun onEvent(event: GenericEvent) {
        if (event is GuildMessageUpdateEvent) {
            onMessageUpdate(event)
            runBlocking {
                FilterUtil.filterDefault(container, event.message)
            }
        }
    }

    private fun onMessageUpdate(event: GuildMessageUpdateEvent) = runBlocking {
        val message = event.message
        val newContent = event.message.contentRaw
        val messageWrapper = container.daoManager.messageHistoryWrapper
        val deferredMessage = async { messageWrapper.getMessageById(event.messageIdLong) }
        val daoMessage = deferredMessage.await()
            ?: DaoMessage(
                event.guild.idLong,
                event.channel.idLong,
                event.author.idLong,
                message.idLong,
                newContent,
                message.timeCreated.toInstant().toEpochMilli()
            )
        val oldContent = daoMessage.content
        daoMessage.content = newContent
        container.taskManager.async {
            messageWrapper.setMessage(daoMessage)
        }

        val channelId = container.daoManager.logChannelWrapper.logChannelCache.get(Pair(event.guild.idLong, LogChannelType.EDITED_MESSAGE)).await()
        if (channelId == -1L) return@runBlocking
        val channel = event.guild.getTextChannelById(channelId) ?: return@runBlocking

        postMessageUpdateLog(event, channel, daoMessage, oldContent)
    }

    private suspend fun postMessageUpdateLog(event: GuildMessageUpdateEvent, logChannel: TextChannel, daoMessage: DaoMessage, oldContent: String) {
        val embedBuilder = EmbedBuilder()
        val language = getLanguage(container.daoManager, -1, event.guild.idLong)
        val title = i18n.getTranslation(language, "listener.message.update.log.title")
            .replace(PLACEHOLDER_CHANNEL, event.channel.asTag)

        val description = i18n.getTranslation(language, "listener.message.update.log.description")
            .replace("%oldContent%", escapeForLog(oldContent))
            .replace("%newContent%", escapeForLog(daoMessage.content))
            .replace(PLACEHOLDER_USER, event.author.asTag)
            .replace(PLACEHOLDER_USER_ID, event.author.id)
            .replace("%sentTime%", event.message.timeCreated.asLongLongGMTString())
            .replace("%editedTime%", System.currentTimeMillis().asEpochMillisToDateTime())
            .replace("%link%", "https://discordapp.com/channels/${event.guild.id}/${event.channel.id}/${event.message.id}")

        embedBuilder.setColor(Color.decode("#A1DAC3"))
        embedBuilder.setThumbnail(event.author.effectiveAvatarUrl)

        embedBuilder.setTitle(title)

        if (description.length > 2048) {
            val parts = StringUtils.splitMessageWithCodeBlocks(description, lang = "LDIF").toMutableList()
            embedBuilder.setDescription(parts[0])
            sendEmbed(container.daoManager.embedDisabledWrapper, logChannel, embedBuilder.build())
            embedBuilder.setThumbnail(null)
            parts.removeAt(0)

            for (part in parts) {
                embedBuilder.setDescription(part)
                sendEmbed(container.daoManager.embedDisabledWrapper, logChannel, embedBuilder.build())
            }
        } else {
            embedBuilder.setDescription(description)

            sendEmbed(container.daoManager.embedDisabledWrapper, logChannel, embedBuilder.build())
        }
    }
}