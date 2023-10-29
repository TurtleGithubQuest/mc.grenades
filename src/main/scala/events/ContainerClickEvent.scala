package dev.turtle.grenades
package events

import dev.turtle.grenades.events.ContainerClickEvent.handlerList
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.{Cancellable, Event, HandlerList}

import scala.annotation.static

class ContainerClickEvent(e: InventoryClickEvent) extends InventoryClickEvent(
  e.getView, e.getSlotType, e.getSlot, e.getClick, e.getAction) {
  override def getHandlers: HandlerList = handlerList

  override def setCancelled(cancelled: Boolean): Unit = e.setCancelled(cancelled)
}
object ContainerClickEvent:
  private val handlerList: HandlerList = new HandlerList
  def getHandlerList(): HandlerList = handlerList
