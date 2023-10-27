package dev.turtle.grenades
package explosions

import explosions.base.GrenadeExplosion
import utils.Blocks.ShrimpleBlock

import org.bukkit.block.Block
import org.bukkit.{Location, Material, World}

object AntiMatter extends GrenadeExplosion {
  override def detonate(loc: Location, blocks: Array[Block]): Boolean = {
    val world: World = blocks(0).getWorld
    this.blockMap = blocks.map {
      block =>
        val finalY = loc.getY+(loc.getY - block.getY)
        ShrimpleBlock(block, Material.AIR)
        ShrimpleBlock(world.getBlockAt(block.getX, finalY.toInt, block.getZ), block.getType)
    }
    true
  }
}
