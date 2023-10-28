package dev.turtle.grenades
package explosions

import enums.DropLocation
import explosions.base.ExplosionType
import utils.Blocks.ShrimpleBlock

import org.bukkit.block.Block
import org.bukkit.inventory.InventoryHolder
import org.bukkit.{Location, Material}

class Classic(di: Integer, dl: Array[DropLocation], ex: String) extends ExplosionType(di, dl, ex) {
  override def filterBlocks(loc: Location, blocks: Array[Block], source:InventoryHolder=null): Array[ShrimpleBlock] = {
    blocks.map {
      block =>
        ShrimpleBlock(block, Material.AIR, dropItems, dropLocations, source)
    }
  }
}
