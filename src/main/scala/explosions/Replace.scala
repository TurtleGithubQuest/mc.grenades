package dev.turtle.grenades
package explosions

import explosions.base.GrenadeExplosion
import utils.Blocks.ShrimpleBlock
import utils.Conf.{cConfig, gConfig}

import com.typesafe.config.Config
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import org.bukkit.{Location, Material}

object Replace extends GrenadeExplosion{
  override def detonate(loc: Location, blocks: Array[Block]): Boolean = {
    val material: Material = Material.valueOf(explosionExtra.toUpperCase)
    val cReplacers: Config = /*cConfig.getConfig(s"explosion-type.replace.$params").withFallback(cConfig.getConfig("explosion-type.replace.default"))*/ {
      if (cConfig.isPathPresent(s"explosion-type.replace.$explosionExtra"))
        cConfig.getConfig(s"explosion-type.replace.$explosionExtra")
      else
        cConfig.getConfig(s"explosion-type.replace.default")
    }.withFallback(cConfig.getConfig(s"explosion-type.replace.default"))

    val depth: Integer = cReplacers.getInt("depth")
    val dropItemsConfig: Boolean = cReplacers.getBoolean("drop-items")
    this.blockMap = blocks.filter { block =>
      (depth <= 0 || loc.getY - depth < block.getY) && block.getY <= loc.getY && block.getType != Material.AIR
    }.map { block =>
      if (dropItemsConfig && dropItems > 0) {
        this.droppedItems :+ block.getDrops
      }
      ShrimpleBlock(block, material)
    }
    true
  }
}
