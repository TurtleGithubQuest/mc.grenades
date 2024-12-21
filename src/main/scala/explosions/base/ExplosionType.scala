package dev.turtle.grenades
package explosions.base

import enums.DropLocation
import utils.Blocks
import utils.Blocks.ShrimpleBlock

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.inventory.InventoryHolder

import java.lang.Class.forName
import scala.jdk.CollectionConverters.*

trait ExplosionType {
  def className: String=super.getClass.getSimpleName.toLowerCase.replaceAll("\\$", "")
  def filterBlocks(loc: Location, blocks: Array[Block], source: InventoryHolder=null): Array[ShrimpleBlock]
}

object ExplosionType {
  def fromClass(className: String, dropItems: Integer, dropLocations: Array[DropLocation], extra: String): ExplosionType = {
    val clazz = Class.forName(className)
    val constructor = clazz.getConstructor(classOf[Integer], classOf[Array[DropLocation]], classOf[String])
    constructor.newInstance(dropItems, dropLocations, extra).asInstanceOf[ExplosionType]
  }
}