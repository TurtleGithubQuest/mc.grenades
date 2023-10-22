package dev.turtle.grenades
package explosions

import utils.parts.ExplosionType

import dev.turtle.grenades.utils.Blocks.setBlockType
import org.bukkit.{Location, Material}
import org.bukkit.block.Block
import org.bukkit.entity.Player

object Classic extends ExplosionType {
  override def detonate(loc: Location, blocks: Array[Block], originName: String, params: String): Boolean = {
    for (block: Block <- blocks) {
      setBlockType(block, Material.AIR, true, originName=originName)
    }
    true
  }
}
