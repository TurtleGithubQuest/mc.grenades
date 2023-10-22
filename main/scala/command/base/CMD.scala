package dev.turtle.grenades

package command.base

import utils.Conf.cConfig

import com.typesafe.config.{Config, ConfigValue}
import dev.turtle.grenades.Main.debugMode
import dev.turtle.grenades.command.Help
import dev.turtle.grenades.utils.Permissions
import dev.turtle.grenades.utils.Permissions.cCommands
import dev.turtle.grenades.utils.lang.Message.sendMessage
import org.bukkit.Bukkit.{getConsoleSender, getLogger}
import org.bukkit.command.{Command, CommandSender, TabExecutor}

import java.util
import java.util.Map.Entry
import scala.collection.immutable.Map
import scala.collection.{immutable, mutable}
import scala.collection.mutable.Map
import scala.jdk.CollectionConverters.*
import scala.util.control.Breaks.{break, breakable}

trait CMD {
  def execute(sender: CommandSender, args: Array[String]): Boolean
  def suggestions(sender: CommandSender, args: Array[String]): Array[String]
  def usage: String
}

object CMD extends TabExecutor {
  def createCommand(fn: (CommandSender, Array[String]) => Boolean): (CommandSender, Array[String]) => Boolean = fn

  var commands: mutable.Map[String, CMD] = mutable.Map()
  val commandsClass: mutable.Map[String, CMD] = mutable.Map(
    "help" -> command.Help,
    "give" -> command.Give,
    "reload" -> command.Reload
  )

  def reload(): Unit = {
    val cCommands: Config = cConfig.getConfig("commands")
    val section_Commands = cCommands.root().keySet().asScala
    var loadedAliases: Int = 0
    for (key <- section_Commands) {
      if (commandsClass.contains(key)) {
        val section_Cmd = cCommands.getConfig(key)
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
    getLogger.info(s"Loaded ${commands.size-loadedAliases} commands and ${loadedAliases} aliases")
  }
  reload()
  override def onCommand(s: CommandSender, cmd: Command, label: String, args: Array[String]): Boolean = {
    val argLength: Int = args.length
    if (argLength > 0) {
      val arg0: String = args(0).toLowerCase
      if (commands.contains(arg0)){
        val o_cmd: CMD = commands(arg0)
        val permission: String = {
          if (cCommands.getIsNull(args(0))) {
            cCommands.getString(s"${CMD.getClass.getName.toLowerCase}.alias.$arg0.permission")
          } else {
            cCommands.getString(s"$arg0.permission")
          }
        }
        val hasPerm: Boolean = Permissions.has(s, permission)
        if (hasPerm) {
          val result: Boolean = o_cmd.execute(s, args)
          return result
        } else {
          sendMessage(s, "commands.no-perm", immutable.Map("command" -> args(0)))
          if (debugMode) sendMessage(getConsoleSender, "console.notify.no-perm", immutable.Map(
            "command" -> args(0),
            "sender" -> s.getName,
            "permission" -> permission
          ))
        }
      }
      return true
    } else {
      Help.helpPage(s)
      return true
    }
    false
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
