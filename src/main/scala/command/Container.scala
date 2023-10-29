package dev.turtle.grenades
package command

import command.base.CMD

import dev.turtle.grenades.container.Editor
import dev.turtle.grenades.utils.Conf
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object Container extends CMD {
  override def execute(s: CommandSender, args: Array[String]): Boolean = {
    if (args.length == 1) {
      s.asInstanceOf[Player].openInventory(new Editor("Test", 9).getInventory)
    }
    true
  }

  override def suggestions(sender: CommandSender, args: Array[String]): Array[String] = Array("editor")

  override def usage: String = "container [container]"
}
