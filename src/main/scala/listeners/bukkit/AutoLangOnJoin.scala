package dev.turtle.grenades
package listeners.bukkit

import listeners.base.ExtraListener
import utils.Conf.configs
import utils.extras.ExtraCommandSender
import utils.lang.Message.clientLang

import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.{EventHandler, EventPriority}

import scala.collection.immutable

class AutoLangOnJoin extends ExtraListener, ExtraCommandSender {
  @EventHandler(priority = EventPriority.LOWEST)
  private def onJoin(e: PlayerJoinEvent): Unit = {
    if (!clientLang.contains(e.playerName)) {
      val playerLocale: String = e.getPlayer.getLocale
      if (configs("lang").hasPath(playerLocale)) {
        clientLang(e.playerName) = playerLocale
        e.getPlayer.sendMessage(
          "lang.auto",
          immutable.Map("locale" -> playerLocale)
        )
      }
    }
  }
}
