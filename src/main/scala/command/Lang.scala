package dev.turtle.grenades
package command

import command.base.CMD
import utils.Conf.*
import utils.extras.ExtraConfig
import utils.lang.Message.clientLang
import utils.optimized.OnlinePlayers

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

object Lang extends CMD {
  override def execute(s: CommandSender, args: Array[String]): Boolean = {
    var placeholders: Map[String, String] = Map(
      "lang" -> clientLang(s.getName)
    )
    if (args.length == 1) {
      s.sendMessage("commands.lang.current.sender", placeholders)
    } else if (args.length > 1) {
      placeholders = Map("lang" -> args(1))
      if (cLang.isPathPresent(args(1))) {
        if (args.length == 2) {
          val permission: String = cCommands.findPermission(args(0), className, perm = "update.self")
          if (s.hasPerm(permission)) {
            clientLang(s.getName) = args(1)
            s.sendMessage("commands.lang.updated.target", placeholders)
          } else {
            placeholders = Map("permission" -> permission, "command" -> args(0))
            s.sendMessage("commands.no-perm", placeholders)
          }
        } else if (args.length == 3) {
          val permission: String = cCommands.findPermission(args(0), className, perm = "update.others")
          if (s.hasPerm(permission)) {
          val target = Bukkit.getPlayer(args(2))
          placeholders = placeholders.updated("target", args(2))
          if (target == null) {
            s.sendMessage("commands.not-found.target", placeholders)
          } else {
            clientLang(args(2)) = args(1)
            s.sendMessage("commands.lang.updated.sender", placeholders)
            target.sendMessage("commands.lang.updated.target", placeholders)
          }
        } else {
            placeholders = Map("permission" -> permission, "command" -> args(0))
            s.sendMessage("commands.no-perm", placeholders)
          }
        }
      } else s.sendMessage("commands.not-found.lang", placeholders)
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
