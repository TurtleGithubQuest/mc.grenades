package dev.turtle.grenades
package utils.extras

import dev.turtle.grenades.Main.plugin
import dev.turtle.grenades.events.bukkit.{AutoLangOnJoin, InteractEvent, LandmineEvents}
import dev.turtle.grenades.utils.Conf.cConfig
import dev.turtle.grenades.utils.optimized.OnlinePlayers
import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerEvent
import scala.util.{Try, Success, Failure}
trait ExtraListener_T extends Listener {
  def configurationPath: String
  implicit class BetterPlayerEvent(e: PlayerEvent) {
    def playerName: String = e.getPlayer.getName
  }
}
class ExtraListener(val configurationPath: String="none") extends ExtraListener_T {}

object Listener {
  val events: Array[ExtraListener] = Array(
    new AutoLangOnJoin("automatic-language-on-join"),
    OnlinePlayers,
    new InteractEvent,
    new LandmineEvents("landmine")
  )
  def registerAllEvents: Unit = {
    for (event <- events) {
      val listenerEnabled: Try[Boolean] = Try(cConfig.getBoolean(s"listeners.${event.configurationPath}"))
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
