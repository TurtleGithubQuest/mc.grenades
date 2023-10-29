package dev.turtle.grenades
package container.base

import enums.SlotAction
import events.ContainerClickEvent
import utils.Conf.cContainer

import com.typesafe.config.Config
import org.bukkit.Material
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.{Inventory, InventoryHolder, ItemStack}

import scala.collection.mutable
import scala.collection.mutable.Map
trait ContainerHolder extends InventoryHolder{
  def className: String=super.getClass.getSimpleName.toLowerCase.replaceAll("\\$", "")
  def classConfig: Config=cContainer.getConfig(className)
  def title: String
  def size: Integer
  def path: String
  //def content: mutable.Map[Integer, ItemStack]=Map()
  def onContainerClick(event: ContainerClickEvent): Unit
  def inventory: Inventory
  //def setSize(newSize: Integer): Boolean
}
case class Item(material: Material) {
  private var itemStack: ItemStack = new ItemStack(material)
  private var itemMeta: ItemMeta = itemStack.getItemMeta
  def amount(amount: Integer): this.type = {
    itemStack.setAmount(amount)
    this
  }
  def displayName(displayName: String): this.type = {
    itemMeta.setDisplayName(displayName)
    this
  }

  def build: ItemStack = {
    itemStack.setItemMeta(itemMeta)
    itemStack
  }
}
/*object Item {
  def apply(material: Material): Item = new Item(material)
}*/
case class ContainerSlot(slotAction: SlotAction, value: Any, metadata: Any, inquire: Any, item: Item)