package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.getDurationString
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withSafeVariable
import me.melijn.melijnbot.internals.utils.withVariable

class DonateCommand : AbstractCommand("command.donate") {

    init {
        id = 97
        name = "donate"
        aliases = arrayOf("patreon", "patron", "premium", "donator", "subscribe")
        children = arrayOf(LinkServerArg(root))
        commandCategory = CommandCategory.UTILITY
    }

    class LinkServerArg(parent: String) : AbstractCommand("$parent.linkserver") {

        init {
            name = "linkServer"
            aliases = arrayOf("lg", "linkGuild", "ls")
            runConditions = arrayOf(RunCondition.GUILD, RunCondition.USER_SUPPORTER)
        }

        override suspend fun execute(context: CommandContext) {
            val wrapper = context.daoManager.supporterWrapper
            val supporter = wrapper.getSupporter(context.authorId) ?: return
            val devIds = context.container.settings.botInfo.developerIds
            if (supporter.lastServerPickTime <= System.currentTimeMillis() - 1_209_600_000 || devIds.contains(supporter.userId)) {
                wrapper.setGuild(context.authorId, context.guildId)

                val msg = context.getTranslation("$root.selected")
                    .withSafeVariable("server", context.guild.name)
                sendRsp(context, msg)
            } else {
                val msg = context.getTranslation("$root.oncooldown")
                    .withVariable(
                        "timeLeft",
                        getDurationString(supporter.lastServerPickTime - (System.currentTimeMillis() - 1_209_600_000))
                    )
                sendRsp(context, msg)
            }
        }
    }

    override suspend fun execute(context: CommandContext) {
        val msg = context.getTranslation("$root.response")
            .withVariable("url", "https://patreon.com/melijn")
        sendRsp(context, msg)
    }
}