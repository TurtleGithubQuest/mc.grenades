package dev.turtle.grenades
package container

import container.base.{ContainerHolder, ContainerSlot, Item}
import utils.extras.ExtraConfig
import enums.SlotAction
import enums.SlotAction.{Inquire, OpenContainer}
import events.ContainerClickEvent

import com.typesafe.config.Config
import dev.turtle.grenades.utils.Conf.{cContainer, cGrenades}
import org.bukkit.{Bukkit, Material}
import org.bukkit.entity.HumanEntity
import org.bukkit.inventory.Inventory

import scala.collection.immutable

class Editor(var title: String, var size: Integer, var path: String="main") extends ContainerHolder {
  override val inventory: Inventory = Bukkit.createInventory(this, size, title)
  override def getInventory: Inventory = inventory
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
      val slotAction: SlotAction = containerSlot.slotAction
      slotAction match {
        case OpenContainer =>
          openContainer(e.getWhoClicked, containerSlot.value.toString)
        case Inquire =>
          inquire(containerSlot.value.toString, containerSlot.metadata.toString, containerSlot.inquire.toString)
      }
    }
    this.getInventory
  }
  private def inquire(value: String, metadata: String, inquireData: String): Unit = {
    val valueSplit = inquireData.split("\\|")
    val inquireMap = valueSplit.foldLeft(immutable.Map.empty[String, String]) {
      (map, pair) =>
        val keyValue = pair.split(":")
        if (keyValue.length > 1) {
          val key = keyValue(0).trim
          val value = keyValue(1).trim
          map.updated(key, value)
        } else {
          map
        }
    }
    if (inquireMap.contains("config")) {
      val config: Config =
        inquireMap("config") match
          case "grenades" =>
            cGrenades
      val path: String = inquireMap("path").replaceAll("%metadata%", metadata)
    }
  }
  private def openContainer(humanEntity: HumanEntity, value: String): Unit = {
    val valueSplit = value.split("\\|")
    val newContainer: String = valueSplit(0)
    val desiredPath: String = valueSplit(1)
    val newClassConfig = cContainer.getConfig(s"$newContainer.$desiredPath")
    val newSize = newClassConfig.findInt("size", default=this.size)
    val newTitle = newClassConfig.findString("title", default=path)
    if (newSize ne this.size) {
      humanEntity.closeInventory()
      humanEntity.openInventory(new Editor(newTitle,newSize,desiredPath).inventory)
    } else {
      this.title = newTitle
      this.path = desiredPath
      this.refreshContents
    }
  }
}
