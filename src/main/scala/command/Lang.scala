package dev.turtle.grenades
package command

import command.base.CMD

import dev.turtle.grenades.Main.pluginPrefix
import dev.turtle.grenades.utils.Conf.cLang
import dev.turtle.grenades.utils.Permissions
import dev.turtle.grenades.utils.Permissions.cCommands
import dev.turtle.grenades.utils.lang.Message.{clientLang, debugMessage, sendMessage}
import dev.turtle.grenades.utils.optimized.OnlinePlayers
import org.bukkit.Bukkit
import org.bukkit.Bukkit.getConsoleSender
import org.bukkit.command.CommandSender

object Lang extends CMD{
  val cmdName: String = CMD.getClass.getName.toLowerCase
  override def execute(s: CommandSender, args: Array[String]): Boolean = {
    var placeholders = Map(
      "lang" -> clientLang(s.getName)
    )
    if (args.length == 1) {
      sendMessage(s, "commands.lang.current.sender", placeholders)
      return true
    }
    if (args.length > 1) {
      placeholders = Map("lang" -> args(1))
      if (!cLang.hasPath(args(1)))
        sendMessage(s, "commands.not-found.lang", placeholders)
        return true
      if (args.length == 2) {
        val permission: String = Permissions.get(args(0), cmdName, perm="update.self")
        if (!Permissions.has(s, permission)) {
          placeholders = Map("permission" -> permission, "command" -> args(0))
          sendMessage(s, "commands.no-perm", placeholders)
          return true
        }
        clientLang(s.getName) = args(1)
        sendMessage(s, "commands.lang.updated.target", placeholders)
      } else if (args.length == 3) {
        val permission: String = Permissions.get(args(0), cmdName, perm="update.others")
        if (!Permissions.has(s, permission)) {
          placeholders = Map("permission" -> permission, "command" -> args(0))
          sendMessage(s, "commands.no-perm", placeholders)
          return true
        }
        val target = Bukkit.getPlayer(args(2))
        placeholders = placeholders.updated("target", args(2))
        if (target == null) {
          sendMessage(s, "commands.not-found.target", placeholders)
        } else {
          clientLang(args(2)) = args(1)
          sendMessage(s, "commands.lang.updated.sender", placeholders)
          sendMessage(target, "commands.lang.updated.target", placeholders)
        }
      }
    }

    true
  }

  override def suggestions(s: CommandSender, args: Array[String]): Array[String] = {
    val argsLength: Integer = args.length
    var suggest: Array[String] = new Array[String](0)
    argsLength match
      case 2 =>
        suggest = cLang.root.keySet().toArray().map(_.toString)
      case 3 =>
        suggest = OnlinePlayers.get
    suggest
  }

  override def usage: String = {
    "lang [lang] [player]"
  }
}
