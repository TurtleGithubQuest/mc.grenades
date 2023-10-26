package dev.turtle.grenades
package utils.extras

import utils.Conf.cLang
import utils.lang.Message
import utils.lang.Message.{clientLang, placeholderPrefix, textPrefix}

import dev.turtle.grenades.utils.Permissions
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

import scala.collection.immutable

object ExtraCommandSender {
  implicit class ExtraCommandSender(s: CommandSender) {
    def hasPerm(permission: String): Boolean = {
      Permissions.has(s, permission)
    }
    def sendMessage(path: String, placeholders: immutable.Map[String, String], chatMessageType: ChatMessageType = ChatMessageType.CHAT): Boolean = {
      var text: String = {
        try {
          textPrefix + cLang.getString(s"${clientLang(s.getName)}.$path")
        } catch {
          case _: Throwable => path
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
