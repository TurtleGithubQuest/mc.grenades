package dev.turtle.grenades
package listeners.bukkit

import container.base.ContainerHolder
import events.ContainerClickEvent
import listeners.base.ExtraListener

import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.{EventHandler, EventPriority}
import org.bukkit.inventory.InventoryHolder

class InventoryClick extends ExtraListener {

  @EventHandler(priority = EventPriority.HIGH)
  private def inventoryClickEvent(e: InventoryClickEvent): Unit = {
    val inventoryHolder: InventoryHolder = {
      if (e.getClickedInventory eq null)
        e.getInventory.getHolder
      else
        e.getClickedInventory.getHolder
    }
    if (inventoryHolder.isInstanceOf[ContainerHolder]) {
      pluginManager.callEvent(new ContainerClickEvent(e))
    }
  }
}
