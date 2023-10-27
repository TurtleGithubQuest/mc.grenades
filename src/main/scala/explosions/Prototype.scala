package dev.turtle.grenades
package explosions

import explosions.base.GrenadeExplosion
import utils.Blocks.ShrimpleBlock

import org.bukkit.block.Block
import org.bukkit.{Location, Material, World}

class Prototype extends GrenadeExplosion {
  override def detonate(loc: Location, blocks: Array[Block]): Boolean = {
    var offsetY = 0
    val previousBlock: Block = blocks(0)
    val world: World = loc.getWorld
    this.blockMap = blocks.map {
      block =>
        block.getLocation.setY(loc.getY)
        if (previousBlock.getY < block.getY) offsetY += 1
        else offsetY = 0
        ShrimpleBlock(world.getBlockAt(block.getX, block.getY+offsetY, block.getZ), block.getType)
        ShrimpleBlock(block, Material.AIR)
    }
    true
  }
}
