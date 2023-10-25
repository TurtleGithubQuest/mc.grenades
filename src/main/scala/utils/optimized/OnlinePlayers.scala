package dev.turtle.grenades
package utils.optimized

import org.bukkit.Bukkit
import org.bukkit.event.player.{PlayerJoinEvent, PlayerQuitEvent}
import org.bukkit.event.{EventHandler, EventPriority, Listener}

import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters._

object OnlinePlayers extends Listener{
  private var array: ArrayBuffer[String] = new ArrayBuffer[String]
  array ++= Bukkit.getOnlinePlayers.asScala.map(_.getName)

  def get: Array[String] = {
    array.toArray
  }
  @EventHandler(priority = EventPriority.LOWEST)
  private def onJoin(e: PlayerJoinEvent): Unit = {
    array += e.getPlayer.getName
  }

  @EventHandler(priority = EventPriority.LOWEST)
  private def onQuit(e: PlayerQuitEvent): Unit = {
    array -= e.getPlayer.getName
  }
}
