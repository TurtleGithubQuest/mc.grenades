package dev.turtle.grenades
package listeners.custom

import events.ContainerClickEvent
import listeners.base.ExtraListener

import dev.turtle.grenades.container.Editor
import dev.turtle.grenades.container.base.ContainerHolder
import org.bukkit.Bukkit
import org.bukkit.event.{EventHandler, EventPriority}
class ContainerClick extends ExtraListener{
  @EventHandler(priority = EventPriority.HIGH)
  private def containerClick(e: ContainerClickEvent): Unit = {
    //if (e.getClickedInventory eq ContainerType)
    e.getInventory.getHolder.asInstanceOf[ContainerHolder].onContainerClick(e)
  }
}
