package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.sendMsg
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject

class TestCommand : AbstractCommand("command.test") {

    init {
        id = 20
        name = "test"
        commandCategory = CommandCategory.DEVELOPER
    }

    override suspend fun execute(context: CommandContext) {
        val daoManager = context.daoManager
        val driverManager = context.daoManager.driverManager

        val wrapper = daoManager.voteWrapper
        val rows = DataArray.empty()
        val table = "votes"
        driverManager.executeMySQLQuery("SELECT * FROM $table", { rs ->

            while (rs.next()) {
                val obj = DataObject.empty()

                obj.put("userId", rs.getLong("userId"))
                obj.put("votes", rs.getLong("votes"))
                obj.put("streak", rs.getLong("streak"))
                obj.put("lastTime", rs.getLong("lastTime"))
                //obj.put("mode", rs.getString("mode"))

                rows.add(obj)
            }
        })

        for (i in 0 until rows.length()) {
            val row = rows.getObject(i)

            wrapper.setVote(
                row.getLong("userId"),
                row.getLong("votes"),
                row.getLong("streak"),
                row.getLong("lastTime")
            )

            context.logger.info("Migrating $table row ${i + 1}/${rows.length()}")
        }
        sendMsg(context, "Migrated $table")
    }
}