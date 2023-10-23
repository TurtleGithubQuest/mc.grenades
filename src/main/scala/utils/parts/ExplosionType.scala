package dev.turtle.grenades
package utils.parts

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Player


trait ExplosionType {
  def detonate(loc: Location, blocks: Array[Block], originName: String, params:String=""): Boolean
}
