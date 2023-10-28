package dev.turtle.grenades
package utils.extras

import utils.Conf.cLang
import utils.extras.ExtraConfig
import utils.lang.Message
import utils.lang.Message.{clientLang, debugMessage, defaultLang, placeholderPrefix, textPrefix}

import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

import scala.collection.immutable

trait ExtraCommandSender {
  implicit class ExtraCommandSender(s: CommandSender) {
    def hasPerm(permission: String): Boolean = {
      val permParts = permission.split("\\.")
      val permGroup = s"${permParts(0)}.${permParts(1)}"
      var hasPerm = s.hasPermission(permission)
      if (permParts.length > 2)
        if (s.hasPermission(s"$permGroup.*") ||
          s.hasPermission(s"$permGroup")
        )
          hasPerm = true
      if (!hasPerm)
        debugMessage("console.notify.no-perm", Map("sender" -> s.getName))
      hasPerm
    }

    def sendMessage(path: String, placeholders: immutable.Map[String, String], chatMessageType: ChatMessageType = ChatMessageType.CHAT): Boolean = {
      var text: String = {
        try {
          textPrefix + cLang.findString(s"${
            clientLang.getOrElse(s.getName, defaultLang)
          }.$path")
        } catch {
          case e: Throwable =>
            path
        }
      }
      for ((key, value) <- placeholders) {
        text = text.replace(s"%$key%", {
          if (value.contains("&")) value
          else placeholderPrefix + value
        } + textPrefix)
      }
      val translatedText: String = ChatColor.translateAlternateColorCodes('&', text)
      s match
        case player: Player => player.spigot().sendMessage(chatMessageType, new TextComponent(translatedText))
        case _ => s.sendMessage(translatedText)
      true
    }
  }
}
