package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.getColorFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.toHex

class ColorCommand : AbstractCommand("command.color") {

    init {
        id = 132
        name = "color"
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }
        val color = getColorFromArgNMessage(context, 0) ?: return

        val hexTitle = context.getTranslation("$root.eb.hex.title")
        val rgbTitle = context.getTranslation("$root.eb.rgb.title")
        val decTitle = context.getTranslation("$root.eb.dec.title")

        val eb = Embedder(context)
            .setColor(color)
            .addField(hexTitle, color.toHex(), true)
            .addField(rgbTitle, "(${color.red}, ${color.green}, ${color.blue})", true)
            .addField(decTitle, color.rgb.toString(), true)
        sendEmbedRsp(context, eb.build())
    }
}