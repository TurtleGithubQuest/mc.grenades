package dev.turtle.grenades
package utils.extras

import dev.turtle.grenades.utils.lang.Message.debugMessage
import org.bukkit.Material
import org.bukkit.block.{Block, BlockState, Container}
import org.bukkit.inventory.{InventoryHolder, ItemStack}

import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters.*

implicit class ExtraBlock(block: Block) {
  private var itemsBuffer: ArrayBuffer[ItemStack] = ArrayBuffer.empty[ItemStack]
  if (block.getType ne Material.AIR) {
    val blockState: BlockState = block.getState
    if (blockState.isInstanceOf[Container]) {
      itemsBuffer ++= blockState.asInstanceOf[Container].getInventory.getContents.filter(_ != null)
    }
    itemsBuffer ++= block.getDrops.asScala
  }
  def getAllDrops: Array[ItemStack] = itemsBuffer.toArray
}
