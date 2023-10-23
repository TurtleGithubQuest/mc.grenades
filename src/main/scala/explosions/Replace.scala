package dev.turtle.grenades
package explosions

import com.typesafe.config.Config
import utils.Blocks.setBlockType
import utils.Conf.cConfig
import utils.parts.ExplosionType

import org.bukkit.{Bukkit, Location, Material}
import org.bukkit.block.Block

object Replace extends ExplosionType{
  override def detonate(loc: Location, blocks: Array[Block], originName: String, params:String): Boolean = {
    val material: Material = Material.valueOf(params.toUpperCase)
    val cReplacers: Config = /*cConfig.getConfig(s"explosion-type.replace.$params").withFallback(cConfig.getConfig("explosion-type.replace.default"))*/ {
      if (cConfig.hasPath(s"explosion-type.replace.$params"))
        cConfig.getConfig(s"explosion-type.replace.$params")
      else
        cConfig.getConfig(s"explosion-type.replace.default")
    }.withFallback(cConfig.getConfig(s"explosion-type.replace.default"))

    val depth: Integer = cReplacers.getInt("depth")
    val dropItems: Boolean = cReplacers.getBoolean("drop-items")
    for (block <- blocks) {
      if ((depth <= 0 || loc.getY - depth < block.getY) && block.getY <= loc.getY)
        if (block.getType != Material.AIR)
          setBlockType(block, material, dropItems, originName=originName)
    }
    true
  }
}
