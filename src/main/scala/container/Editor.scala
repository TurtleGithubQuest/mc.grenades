package dev.turtle.grenades
package container

import container.base.{ContainerHolder, ContainerSlot, Item}
import utils.extras.ExtraConfig
import enums.SlotAction
import enums.SlotAction.{Inquire, OpenContainer}
import events.ContainerClickEvent

import com.typesafe.config.Config
import dev.turtle.grenades.utils.Conf.{cContainer, cExplosions, cGrenades, grenades}
import org.bukkit.{Bukkit, Material}
import org.bukkit.entity.HumanEntity
import org.bukkit.inventory.Inventory

import scala.collection.immutable

class Editor(var title: String, var size: Integer, var path: String="main") extends ContainerHolder {
  override val inventory: Inventory = Bukkit.createInventory(this, size, title)
  override def getInventory: Inventory = inventory
  override val name = "editor"
  private var content: immutable.Map[Int, ContainerSlot] = immutable.Map().empty
  refreshContents
  def refreshContents: Boolean = {
    content = classConfig.findAllContainerSlots(s"$path")
    for((slot, containerSlot) <- content)
      if (inventory.getSize > slot){
        inventory.setItem(slot, containerSlot.item.build)
      }
    true
  }
  override def onContainerClick(e: ContainerClickEvent): Unit = {
    e.setCancelled(true)
    val containerSlot: ContainerSlot = content.getOrElse(e.getSlot, null)
    if (containerSlot ne null) {
      for ((slotAction, metadata) <- containerSlot.slotActions)
      {
        slotAction match {
          case OpenContainer =>
            openContainer(e.getWhoClicked, metadata)
          case Inquire =>
            inquire(metadata, e.getSlot)
        }
      }
    }
    this.getInventory
  }

  private def inquire(metadata: Map[String, Any], slot: Integer): Unit = {
    if (metadata.contains("config.name")) {
      var valuePath: String = metadata("config.path").toString
      if (valuePath.contains("%slotname%"))
        valuePath = valuePath.replaceAll("%slotname%", getSlotName(slot))
      val config: Config =
        metadata("config.name").toString match
          case "grenades" =>
            cGrenades
          case "explosions" =>
            cExplosions
      val found: String = config.findString(valuePath)
      Bukkit.broadcastMessage(found)
    }
  }
  private def openContainer(humanEntity: HumanEntity, metadata: Map[String, Any]): Unit = {
    val targetContainer: String = metadata("target").toString.replaceAll("%this%", this.name)
    val desiredPath: String = metadata("path").toString
    val newClassConfig = cContainer.getConfig(s"$targetContainer.$desiredPath")
    val newSize = newClassConfig.findInt("size", default=Some(this.size))
    val newTitle = newClassConfig.findString("title", default=Some(path))
    if (newSize ne this.size) {
      humanEntity.closeInventory()
      humanEntity.openInventory(new Editor(newTitle,newSize,desiredPath).inventory)
    } else {
      this.title = newTitle
      this.path = desiredPath
      this.refreshContents
    }
  }
  def getSlotName(slot: Integer): String = cContainer.findString(s"${this.name}.$path.content.$slot.metadata.slotname")
}
