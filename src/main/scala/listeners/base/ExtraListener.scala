package dev.turtle.grenades
package listeners.base

import Main.plugin
import listeners.bukkit.*
import utils.Conf.configs
import utils.extras.ExtraConfig
import utils.optimized.OnlinePlayers

import listeners.custom.ContainerClick
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerEvent
import org.bukkit.plugin.PluginManager

import scala.util.{Success, Try}

trait ExtraListener extends Listener {
  private def className: String=super.getClass.getSimpleName.toLowerCase.replaceAll("\\$", "")
  val pluginManager: PluginManager = plugin.getServer.getPluginManager
  implicit class BetterPlayerEvent(e: PlayerEvent) {
    def playerName: String = e.getPlayer.getName
  }
}

object ExtraListener {
  val events: Array[ExtraListener] = Array(
    new AutoLangOnJoin,
    new OnlinePlayers,
    new InteractEvent,
    new LandmineEvents,
    new InventoryClick,
    new ContainerClick
  )
  def registerAllEvents(): Unit = {
    for (event <- events) {
      val listenerEnabled: Try[Boolean] = Try(configs("config").findBoolean(s"listeners.${event.className}"))
      listenerEnabled match {
        case Success(false) => {}
        case _ =>
          plugin.getServer.getPluginManager.registerEvents(event, plugin)
      }
    }
  }
  def registerEvent(): Boolean = {
    true
  }
}
