package dev.turtle.grenades
package command

import Main.{plugin, pluginPrefix}
import command.base.CMD

import dev.turtle.onelib.command.OneCommand
import dev.turtle.onelib.api.OneLibAPI
import dev.turtle.onelib.message.OneCommandSender
import dev.turtle.onelib.message.{Placeholder, Placeholders}
import dev.turtle.onelib.utils.Exceptions.oneAssert
import org.bukkit
import org.bukkit.{Bukkit, ChatColor}
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object Help extends OneCommand("example", oneLibAPI=plugin.oneLibAPI) {
  //this.Suggestion(0, Array("ll"))
  /**
   *    /example launch <power> <player>
   */
  new Argument("launch") {
    override def onArgument(sender: CommandSender): Boolean = {
      // Cast is already checked for correct type
      val power = getInputValue(0).get.toInt
      //val maxPower = config("example").getInt(path("power.max"))
      val maxPower = plugin.oneLibAPI.config.configName("example").getInt("power.max")
      oneAssert(power <= maxPower, sender.getLocalizedText(Seq("power-limit-reached"), Placeholder("maxpower", maxPower).s))
      val target: Player = Bukkit.getPlayer(getInputValue(1).getOrElse(sender.getName))
      if (target.isOnline) {
        target.setVelocity(new bukkit.util.Vector(0, power, 0))
        sender.sendLocalizedMessage(
          path(sauce="success"),
          Placeholders(
            Placeholder("target", target.getName),
            Placeholder("power", power)
          )
        )
      }
      true
    }
    this.Suggestion(2, "1,10,15,25,50".split(","))
  }.setInputs(
      ArgumentInput("power", classOf[Integer]), ArgumentInput("player", isRequired=false))
    .requiredPermission(path("launch"))
}
/*object Help extends CMD {
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
}*/
