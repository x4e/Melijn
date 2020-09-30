package me.melijn.melijnbot.internals.utils.message

import io.ktor.client.*
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.database.message.ModularMessage
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.utils.ModularPaginationInfo
import me.melijn.melijnbot.internals.utils.PaginationInfo
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.PrivateChannel
import net.dv8tion.jda.api.entities.TextChannel

suspend fun sendPaginationModularRsp(context: CommandContext, modularMessages: List<ModularMessage>, index: Int) {
    val premiumGuild = context.isFromGuild && context.daoManager.supporterWrapper.getGuilds().contains(context.guildId)
    if (premiumGuild) {
        sendPaginationModularRsp(context.textChannel, context.webManager.proxiedHttpClient, context.authorId, context.daoManager, modularMessages, index)
    } else {
        sendPaginationModularMsg(context, modularMessages, index)
    }
}

suspend fun sendPaginationModularRsp(textChannel: TextChannel, httpClient: HttpClient, authorId: Long, daoManager: DaoManager, modularMessages: List<ModularMessage>, index: Int) {
    val msg = modularMessages[index]

    val message = sendRspAwaitN(textChannel, httpClient, daoManager, msg)
        ?: throw IllegalArgumentException("Couldn't send the message")
    registerPaginationModularMessage(textChannel, authorId, message, modularMessages, index)
}

suspend fun sendPaginationMsg(context: CommandContext, msgList: List<String>, index: Int) {
    val msg = msgList[index]
    if (msg.length > 2000) throw IllegalArgumentException("No splitting here :angry:")

    if (context.isFromGuild) {
        val message = sendMsgAwaitEL(context.textChannel, msg).first()
        registerPaginationMessage(context.textChannel, context.authorId, message, msgList, index)
    } else {
        val message = sendMsgAwaitEL(context.privateChannel, msg).first()
        registerPaginationMessage(context.privateChannel, context.authorId, message, msgList, index)
    }
}

suspend fun sendPaginationModularMsg(context: CommandContext, msgList: List<ModularMessage>, index: Int) {
    val msg = msgList[index]

    if (context.isFromGuild) {
        val message = sendMsgAwaitN(context.textChannel, context.webManager.proxiedHttpClient, msg)
            ?: throw IllegalArgumentException("Couldn't send the message")
        registerPaginationModularMessage(context.textChannel, context.authorId, message, msgList, index)
    } else {
        val message = sendMsgAwaitN(context.privateChannel, context.webManager.proxiedHttpClient, msg)
            ?: throw IllegalArgumentException("Couldn't send the message")
        registerPaginationModularMessage(context.privateChannel, context.authorId, message, msgList, index)
    }
}


fun registerPaginationModularMessage(textChannel: TextChannel, authorId: Long, message: Message, msgList: List<ModularMessage>, index: Int) {
    Container.instance.modularPaginationMap[System.nanoTime()] = ModularPaginationInfo(
        textChannel.guild.idLong,
        textChannel.idLong,
        authorId,
        message.idLong,
        msgList,
        index
    )

    addPaginationEmotes(message, msgList.size > 2)
}

fun registerPaginationModularMessage(privateChannel: PrivateChannel, authorId: Long, message: Message, msgList: List<ModularMessage>, index: Int) {
    Container.instance.modularPaginationMap[System.nanoTime()] = ModularPaginationInfo(
        -1,
        privateChannel.idLong,
        authorId,
        message.idLong,
        msgList,
        index
    )

    addPaginationEmotes(message, msgList.size > 2)
}

fun registerPaginationMessage(textChannel: TextChannel, authorId: Long, message: Message, msgList: List<String>, index: Int) {
    Container.instance.paginationMap[System.nanoTime()] = PaginationInfo(
        textChannel.guild.idLong,
        textChannel.idLong,
        authorId,
        message.idLong,
        msgList,
        index
    )

    addPaginationEmotes(message, msgList.size > 2)
}

fun registerPaginationMessage(privateChannel: PrivateChannel, authorId: Long, message: Message, msgList: List<String>, index: Int) {
    Container.instance.paginationMap[System.nanoTime()] = PaginationInfo(
        -1,
        privateChannel.idLong,
        authorId,
        message.idLong,
        msgList,
        index
    )

    addPaginationEmotes(message, msgList.size > 2)
}

fun addPaginationEmotes(message: Message, morePages: Boolean) {
    if (morePages) message.addReaction("⏪").queue()
    message.addReaction("◀️").queue()
    message.addReaction("▶️").queue()
    if (morePages) message.addReaction("⏩").queue()
}