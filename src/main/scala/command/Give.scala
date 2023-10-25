package dev.turtle.grenades
package command

import command.base.CMD

import dev.turtle.grenades.utils.Conf.{cGrenades, grenades}
import dev.turtle.grenades.utils.Grenade
import dev.turtle.grenades.utils.lang.Message.sendMessage
import dev.turtle.grenades.utils.optimized.OnlinePlayers
import org.bukkit.{Bukkit, Material}
import org.bukkit.command.CommandSender
import org.bukkit.inventory.ItemStack

import scala.collection.immutable
import scala.jdk.CollectionConverters._

object Give extends CMD{
  override def execute(s: CommandSender, args: Array[String]): Boolean = {
    if (args.length >= 3) {
      val target = Bukkit.getPlayer(args(1))
      if (!target.isValid){
        sendMessage(s, "commands.not-found.target", Map(
          "target" -> args(1)
        ))
        return true
      }
      if (!grenades.contains(args(2))) {
        sendMessage(s, "commands.not-found.grenade", Map(
          "grenade_id" -> args(2)
        ))
        return true
      }
      val grenade: Grenade = grenades(args(2))
      val itemStack = grenade.item
      if (args.length >= 4)
        itemStack.setAmount(args(3).toIntOption.getOrElse(1))
      target.getInventory.addItem(itemStack)
      val give_map: immutable.Map[String, String] = Map(
        "target" -> args(1),
        "grenade_id" -> grenade.displayName,
        "sender" -> s.getName,
        "amount" -> itemStack.getAmount.toString
      )
      sendMessage(s, "commands.give.sender", give_map)
      sendMessage(target, "commands.give.target", give_map)
    }

    true
  }

  override def suggestions(s: CommandSender, args: Array[String]): Array[String] = {
    val argsLength: Integer = args.length
    var suggest: Array[String] = new Array[String](0)
    argsLength match
      case 2 =>
        suggest = OnlinePlayers.get//Bukkit.getOnlinePlayers.asScala.map(_.getName).toArray
      case 3 =>
        suggest = grenades.keys.toArray
      case 4 =>
        suggest = (for (amount <- Range(0, 65, 16)) yield amount.toString).toArray
        suggest.update(0, "1")
    suggest
  }

  override def usage: String = {
    "give <player> <grenade_id> <amount>"
  }
}
