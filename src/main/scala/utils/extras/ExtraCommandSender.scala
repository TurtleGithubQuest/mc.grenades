package dev.turtle.grenades
package utils.extras

import utils.Conf.configs
import utils.extras.ExtraConfig
import utils.lang.Message
import utils.lang.Message.{clientLang, debugMessage, defaultLang, getLocalizedText, placeholderPrefix, textPrefix}

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
    def getLanguage: String = clientLang.getOrElse(s.getName, defaultLang)

    def sendMessage(path: String, placeholders: immutable.Map[String, String], chatMessageType: ChatMessageType = ChatMessageType.CHAT): Boolean = {
      val text: String = getLocalizedText(s.getLanguage, path, placeholders)
      s match
        case player: Player => player.spigot().sendMessage(chatMessageType, new TextComponent(text))
        case _ => s.sendMessage(text)
      true
    }
  }
}
