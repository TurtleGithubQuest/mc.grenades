package dev.turtle.grenades
package listeners.bukkit

import utils.Conf.configs
import utils.{Conf, Landmine}
import dev.turtle.grenades.listeners.base.ExtraListener

import org.bukkit.block.Block
import org.bukkit.event.block.Action
import org.bukkit.event.entity.{EntityExplodeEvent, EntityInteractEvent}
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.{EventHandler, EventPriority}

class LandmineEvents extends ExtraListener {
  @EventHandler(priority = EventPriority.HIGH)
  private def entityInteractEvent(e: EntityInteractEvent): Unit = {
    if (e.isCancelled) {return}
    Landmine.isPresent(e.getBlock.getLocation)
  }

  @EventHandler(priority = EventPriority.HIGH)
  private def playerInteractEvent(e: PlayerInteractEvent): Unit = {
    if (e.getAction ne Action.PHYSICAL) {return}
    Landmine.isPresent(e.getClickedBlock.getLocation)
  }

  @EventHandler(priority = EventPriority.LOW)
  private def onExplosion(e: EntityExplodeEvent): Unit = {
    if (e.isCancelled || !configs("config").getBoolean("landmine.chain-reactions")) {return}
    e.blockList().forEach({(block: Block) =>
      Landmine.isPresent(block.getLocation)
    })
  }
}
