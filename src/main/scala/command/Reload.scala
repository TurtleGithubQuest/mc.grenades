package dev.turtle.grenades
package command

import command.base.CMD
import utils.Conf

import dev.turtle.grenades.utils.lang.Message.sendMessage
import org.bukkit.command.CommandSender

object Reload extends CMD{
  override def execute(s: CommandSender, args: Array[String]): Boolean = {
    if (args.length == 1) {
      Conf.reload()
    } else if (args.length >= 2){
      if (args(1).equalsIgnoreCase("grenades"))
        Conf.reloadGrenades()
        return true
      if (args(1).equalsIgnoreCase("all"))
        Conf.reload()
    }
    sendMessage(s, "commands.reload.success", Map(
      "config" -> {
        if (args.length > 1) args(1).toLowerCase
        else "all"
      }
    ))
    true
  }

  override def suggestions(sender: CommandSender, args: Array[String]): Array[String] = Array("all", "grenades")

  override def usage: String = "reload [config]"
}
