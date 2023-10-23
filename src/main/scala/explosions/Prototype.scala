package dev.turtle
package explosions

import grenades.utils.parts.ExplosionType

import org.bukkit.{Location, Material, World}
import dev.turtle.grenades.utils.Blocks.setBlockType
import org.bukkit.block.Block

object Prototype extends ExplosionType{
  override def detonate(loc: Location, blocks: Array[Block], originName: String, params: String): Boolean = {
    var offsetY = 0
    val previousBlock: Block = blocks(0)
    val world: World = loc.getWorld
    for (block <- blocks) {
      block.getLocation.setY(loc.getY)
      if (previousBlock.getY < block.getY) offsetY += 1
      else offsetY = 0
      setBlockType(world.getBlockAt(block.getX, block.getY+offsetY, block.getZ), block.getType, false, originName=originName)
      setBlockType(block, Material.AIR, false, originName=originName)
    }
    true
  }
}
