package dev.turtle.grenades

package utils

import org.bukkit.{Bukkit, Location, Material}
import utils.gBlock

import dev.turtle.grenades.Main.coreprotectapi
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent

import scala.math.{pow, sqrt}

trait T_gBlock {
  def block: Block
  def removable: Boolean
}
class gBlock(val block: Block,
             val removable: Boolean
            ) extends T_gBlock


object Blocks {
  def getInRadius(loc: Location, radius: Integer, player: Player = null): Array[Block] = {
    var blocks: Array[Block] = new Array[Block](0)
    for (x <- -radius to radius) {
      for (y <- -radius to radius) {
        for (z <- -radius to radius) {
          val distFromCenter: Int = Math.abs(x)
          val distance = sqrt(pow(x, 2) + pow(y, 2) + pow(z, 2))
          if (distance <= radius) {
            var block: Block = loc.getWorld.getBlockAt(
              loc.getBlockX + x,
              loc.getBlockY + y,
              loc.getBlockZ + z
            )
            if (player != null) {
              if (canDestroyThatBlock(player, block))
                blocks = blocks :+ block
            } else blocks = blocks :+ block
          }
        }
      }
    }
    return blocks
  }

  def canDestroyThatBlock(player: Player, block: Block): Boolean = {
    val breakEvent = new BlockBreakEvent(block, player)
    Bukkit.getServer.getPluginManager.callEvent(breakEvent)
    if (breakEvent.isCancelled) return false
    true
  }

  def setBlockType(b: Block, newType: Material, dropItems: Boolean, originName: String = "unknown"): Unit = {
    if (coreprotectapi ne null) {
      if (newType ne Material.AIR)
        coreprotectapi.logPlacement(originName, b.getLocation, newType, b.getBlockData)
      else {
        coreprotectapi.logRemoval(originName, b.getLocation, b.getType, b.getBlockData)
      }
    }
    if (dropItems && (newType eq Material.AIR)) b.breakNaturally
    b.setType(newType)
  }
}
