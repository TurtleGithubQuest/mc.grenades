package dev.turtle.grenades
package explosions

import enums.DropLocation
import explosions.base.ExplosionType
import utils.Blocks.ShrimpleBlock

import org.bukkit.block.Block
import org.bukkit.inventory.InventoryHolder
import org.bukkit.{Location, Material, World}

class AntiMatter(dropItems: Integer, dropLocations: Array[DropLocation], ex: String) extends ExplosionType {
  override def filterBlocks(loc: Location, blocks: Array[Block], source:InventoryHolder=null): Array[ShrimpleBlock] = {
    val world: World = loc.getWorld
    blocks.map {
      block =>
        val finalY = loc.getY+(loc.getY - block.getY)
        ShrimpleBlock(block, Material.AIR, dropItems, dropLocations, source)
        ShrimpleBlock(world.getBlockAt(block.getX, finalY.toInt, block.getZ), block.getType, dropItems, dropLocations, source)
    }
  }
}
