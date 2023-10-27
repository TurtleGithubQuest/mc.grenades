package dev.turtle.grenades
package explosions

import explosions.base.GrenadeExplosion
import utils.Blocks.ShrimpleBlock

import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import org.bukkit.{Location, Material}

object Classic extends GrenadeExplosion {
  override def detonate(loc: Location, blocks: Array[Block]): Boolean = {
    this.blockMap = blocks.map {
      block =>
        if (this.dropItems > 0)
          this.droppedItems :+ block.getDrops
        ShrimpleBlock(block, Material.AIR)
    }
    true
  }
}
