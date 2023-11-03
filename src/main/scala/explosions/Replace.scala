package dev.turtle.grenades
package explosions

import explosions.base.ExplosionType
import utils.Blocks.ShrimpleBlock
import utils.Conf.configs
import utils.extras.ExtraConfig

import com.typesafe.config.Config
import dev.turtle.grenades.enums.DropLocation
import org.bukkit.block.Block
import org.bukkit.inventory.InventoryHolder
import org.bukkit.{Location, Material}

class Replace(dropItems: Integer, dropLocations: Array[DropLocation], extra: String) extends ExplosionType {
  override def filterBlocks(loc: Location, blocks: Array[Block], source:InventoryHolder=null): Array[ShrimpleBlock] = {
    val material: Material = Material.valueOf(extra.toUpperCase)
    val cReplacers: Config = configs("config").getOrElse(s"explosion-type.replace.$extra", "explosion-type.replace.default") /*cConfig.getConfig(s"explosion-type.replace.$params").withFallback(cConfig.getConfig("explosion-type.replace.default"))*/

    val depth: Integer = cReplacers.getInt("depth")
    val dropItemsConfig: Boolean = cReplacers.getBoolean("drop-items")
    blocks.filter { block =>
      (depth <= 0 || loc.getY - depth < block.getY) && block.getY <= loc.getY && block.getType != Material.AIR
    }.map { block =>
      ShrimpleBlock(block, material, dropItems, dropLocations, source)
    }
  }
}
