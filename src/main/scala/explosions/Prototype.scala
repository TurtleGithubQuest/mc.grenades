package dev.turtle.grenades
package explosions

import explosions.base.ExplosionType
import utils.Blocks.ShrimpleBlock

import dev.turtle.grenades.enums.DropLocation
import org.bukkit.block.Block
import org.bukkit.inventory.InventoryHolder
import org.bukkit.{Location, Material, World}

class Prototype(di: Integer, dl: Array[DropLocation], ex: String) extends ExplosionType(di, dl, ex) {
  override def filterBlocks(loc: Location, blocks: Array[Block], source:InventoryHolder=null): Array[ShrimpleBlock] = {
    var offsetY = 0
    val previousBlock: Block = blocks(0)
    val world: World = loc.getWorld
    blocks.map {
      block =>
        block.getLocation.setY(loc.getY)
        if (previousBlock.getY < block.getY) offsetY += 1
        else offsetY = 0
        ShrimpleBlock(world.getBlockAt(block.getX, block.getY+offsetY, block.getZ), block.getType, dropItems, dropLocations, source)
        ShrimpleBlock(block, Material.AIR, dropItems, dropLocations, source)
    }
  }
}
