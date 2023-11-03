package dev.turtle.grenades
package command

import command.base.CMD
import container.Editor
import utils.Conf.configs
import utils.extras.ExtraItemStack

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import utils.extras.ExtraConfig

import dev.turtle.grenades.utils.optimized.OnlinePlayers
import org.bukkit.{Bukkit, Material}
import org.bukkit.inventory.ItemStack

object Container extends CMD {
  override def execute(s: CommandSender, args: Array[String]): Boolean = {
    if (args.length == 1)
      s.sendMessage("commands.container.list", Map("containers" -> configs("container").root.keySet.asScala.mkString(", ")))
    else {
      val placeholders: mutable.Map[String, String] = mutable.Map("container" -> args(1))
      if (configs("container").isPathPresent(args(1))) {
        var target: Player = {
            s match
              case player: Player => player
              case _ => null
          }
        if (args.length >= 3) {
          target = Bukkit.getPlayer(args(2))
          placeholders += ("target" -> args(2))
        }
        if ((target ne null) && (target.isValid)){
          val itemInHand: ItemStack = target.getInventory.getItemInMainHand
          val category = {
            if ((itemInHand.getAmount > 0) && itemInHand.isGrenade) {
              itemInHand.getGrenadeID
            } else "grenades"
          }
          val metadata: mutable.Map[String, Any] = mutable.Map(
            "name" -> args(1),
            "category" -> "grenades",
            "config_key" -> category
          )
          target.openInventory(new Editor(metadata("config_key").toString, configs("container").findInt(s"${args(1)}.$category.size", default=Option(9)), metadataMap = metadata).getInventory)
        } else {
          if (placeholders.contains("target"))
            s.sendMessage("commands.not-found.target", placeholders.toMap)
          else s.sendMessage("commands.console.target-required", placeholders.toMap)
        }
      } else s.sendMessage("commands.container.not-found", placeholders.toMap)
    }
    true
  }

  override def suggestions(s: CommandSender, args: Array[String]): Array[String] = {
    val argsLength: Integer = args.length
    var suggest: Array[String] = new Array[String](0)
    argsLength match
      case 2 =>
        suggest = configs("container").root.keySet.toArray.map(_.toString)
      case 3 =>
        suggest = OnlinePlayers.get
    suggest
  }

  override def usage: String = "container <container> [player]"
}
