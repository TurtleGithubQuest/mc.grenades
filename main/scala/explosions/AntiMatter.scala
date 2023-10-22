package dev.turtle.grenades
package explosions

import utils.parts.ExplosionType

import dev.turtle.grenades.utils.Blocks.setBlockType
import org.bukkit.{Location, Material, World}
import org.bukkit.block.Block
import org.bukkit.entity.Player

object AntiMatter extends ExplosionType {
  override def detonate(loc: Location, blocks: Array[Block], originName: String, params: String=""): Boolean = {
    val world: World = blocks(0).getWorld
    for (block <- blocks) {
      val newY = loc.getY - block.getY
      val finalY = loc.getY + newY
      setBlockType(world.getBlockAt(block.getX, finalY.toInt, block.getZ), block.getType, false, originName=originName)
      setBlockType(block, Material.AIR, false, originName=originName)
    }
    true
  }
}
