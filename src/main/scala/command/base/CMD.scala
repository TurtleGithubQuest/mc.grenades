package dev.turtle.grenades

package command.base

import command.Help
import utils.Conf.configs
import utils.extras.{ExtraCommandSender, ExtraConfig}
import utils.lang.Message.debugMessage

import org.bukkit.command.{Command, CommandSender, TabExecutor}

import java.util
import scala.collection.immutable.Map
import scala.collection.mutable.Map
import scala.collection.{immutable, mutable}
import scala.jdk.CollectionConverters.*
import scala.util.control.Breaks.{break, breakable}

trait CMD extends ExtraCommandSender {
  def execute(sender: CommandSender, args: Array[String]): Boolean
  def suggestions(sender: CommandSender, args: Array[String]): Array[String]
  def usage: String

  def className: String = {
    super.getClass.getSimpleName.toLowerCase.replaceAll("\\$", "")
  }
}

object CMD extends TabExecutor, ExtraCommandSender {
  def createCommand(fn: (CommandSender, Array[String]) => Boolean): (CommandSender, Array[String]) => Boolean = fn

  var commands: mutable.Map[String, CMD] = mutable.Map()
  private val commandsClass: mutable.Map[String, CMD] = mutable.Map(
    "help" -> command.Help,
    "give" -> command.Give,
    "reload" -> command.Reload,
    "lang" -> command.Lang,
    "summon" -> command.Summon,
    "container" -> command.Container
  )

  def reload(): Unit = {
    configs("commands") = configs("config").getConfig("commands")
    val section_Commands = configs("commands").root().keySet().asScala
    var loadedAliases: Int = 0
    for (key <- section_Commands) {
      if (commandsClass.contains(key)) {
        val section_Cmd = configs("commands").getConfig(key)
        if (!section_Cmd.getBoolean("disable")) {
          commands.put(key, commandsClass(key))
        }
        val aliasKeys = section_Cmd.getConfig("alias").root().keySet().asScala
        for (aliasKey <- aliasKeys) {
          breakable {
            if (section_Cmd.hasPath(s"alias.$aliasKey.disable"))
              if (section_Cmd.getBoolean(s"alias.$aliasKey.disable"))
                break
            commands.put(aliasKey, commandsClass(key))
            loadedAliases += 1
          }
        }
      }
    }
    debugMessage(s"Loaded ${commands.size-loadedAliases} commands and ${loadedAliases} aliases.")
  }
  override def onCommand(s: CommandSender, cmd: Command, label: String, args: Array[String]): Boolean = {
    val argLength: Int = args.length
    if (argLength > 0) {
      val arg0: String = args(0).toLowerCase
      if (commands.contains(arg0)){
        val o_cmd: CMD = commands(arg0)
        val permission: String = configs("commands").findPermission(args(0), o_cmd.className)
        if (s.hasPerm(permission)) {
          val result: Boolean = o_cmd.execute(s, args)
          result
        } else {
          s.sendMessage("commands.no-perm", immutable.Map("command" -> args(0)))
          debugMessage("console.notify.no-perm", immutable.Map(
            "command" -> args(0),
            "sender" -> s.getName,
            "permission" -> permission
          ), debugLevel=100)
        }
      } else true
    } else {
      Help.helpPage(s)
      true
    }
  }

  override def onTabComplete(sender: CommandSender, command: Command, label: String, args: Array[String]): util.List[String] = {
    val argsLength: Integer = args.length
    if (argsLength == 1) {
      val list: java.util.List[String] = commands.keys.toList.asJava
      return list
    } else if (argsLength >= 2){
      val arg0: String = args(0).toLowerCase
      if (commands.contains(arg0)) {
        return commands(arg0).suggestions(sender, args).toList.asJava
      }
    }
    null;
  }
}
