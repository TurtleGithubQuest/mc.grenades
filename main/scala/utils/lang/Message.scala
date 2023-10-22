package dev.turtle.grenades
package utils.lang

import dev.turtle.grenades.utils.Conf.{cConfig, cLang}
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

import scala.collection.{immutable, mutable}

object Message {
  private val cMessaging = cConfig.getConfig("general.messaging")
  private val defaultLang = cMessaging.getString("default.lang")
  private val placeholderPrefix = cMessaging.getString("default.prefix.placeholder")
  private val textPrefix = cMessaging.getString("default.prefix.text")
  private val placeholderRegex = "%(.*?)%".r
  var clientLang: mutable.Map[String, String] = mutable.Map().withDefault(k => defaultLang)

  def sendMessage(s: CommandSender, path: String, placeholders: immutable.Map[String, String], chatMessageType: ChatMessageType=ChatMessageType.CHAT): Boolean = {
    var text: String = {
      try {
        textPrefix + cLang.getString(s"$path.${clientLang.getOrElse(s.getName, defaultLang)}")
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
