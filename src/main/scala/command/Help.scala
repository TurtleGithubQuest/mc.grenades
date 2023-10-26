package dev.turtle.grenades
package command

import Main.pluginPrefix
import command.base.CMD

import org.bukkit.ChatColor
import org.bukkit.command.CommandSender

object Help extends CMD {
  override def execute(s: CommandSender, args: Array[String]): Boolean = {
    if (args.length == 1) {
      helpPage(s)
    } else if (args.length > 1) {
      val commandQuery: String = args(1)
      if (CMD.commands.contains(commandQuery)) {
        s.sendMessage(CMD.commands(commandQuery).usage, Map())
      } else helpPage(s)
    }
    true
  }
  def helpPage(s: CommandSender): Unit = {
    s.sendMessage(
      s"&3&l==== $pluginPrefix &3&l===="
      , Map())
    for ((cmdName: String, cmd: CMD) <- CMD.commands) {
      s.sendMessage(s"/g ${cmd.usage}", Map())
    }
    s.sendMessage("")
  }

  override def suggestions(s: CommandSender, args: Array[String]): Array[String] = {
    val argsLength: Integer = args.length
    val suggest: Array[String] = new Array[String](0)
    suggest
  }
  override def usage: String = {
    "help [command]"
  }
}
