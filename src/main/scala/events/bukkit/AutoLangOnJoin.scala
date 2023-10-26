package dev.turtle.grenades
package events.bukkit

import utils.extras.{ExtraCommandSender, ExtraListener}
import utils.lang.Message.clientLang
import utils.Conf.cLang

import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.{EventHandler, EventPriority}

import scala.collection.immutable

class AutoLangOnJoin(override val configurationPath: String) extends ExtraListener(configurationPath), ExtraCommandSender {
  @EventHandler(priority = EventPriority.LOWEST)
  private def onJoin(e: PlayerJoinEvent): Unit = {
    if (!clientLang.contains(e.playerName)) {
      val playerLocale: String = e.getPlayer.getLocale
      if (cLang.hasPath(playerLocale)) {
        clientLang(e.playerName) = playerLocale
        e.getPlayer.sendMessage(
          "lang.auto",
          immutable.Map("locale" -> playerLocale)
        )
      }
    }
  }
}
