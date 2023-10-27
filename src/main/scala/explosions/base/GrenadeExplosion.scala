package dev.turtle.grenades
package explosions.base

import utils.Blocks.ShrimpleBlock
import utils.{Blocks, Grenade}

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Damageable
import org.bukkit.inventory.ItemStack

import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters.*

trait GrenadeExplosion {
  def detonate(loc: Location, blocks: Array[Block]): Boolean//ArrayBuffer[ItemStack]

  var detonationLoc: Location = null
  var blockMap: Array[ShrimpleBlock] = new Array[ShrimpleBlock](0)
  var droppedItems: ArrayBuffer[ItemStack] = ArrayBuffer.empty[ItemStack]
  var grenade: Grenade = null
  var dropItems: Integer = 1
  var damage: Double = 1
  var power: Double = 1
  var explosionExtra: String = ""
  def cleanup(): Boolean = {
    Blocks.addToQueue(detonationLoc.getWorld, blockMap)
    val nearbyEntities = detonationLoc.getWorld.getNearbyEntities(detonationLoc, power, power, power)
    for (entity <- nearbyEntities.asScala) {
      if ((entity ne null) && entity.isInstanceOf[Damageable])
        entity.asInstanceOf[Damageable].damage(damage)
    }
    true
  }
  implicit class ExtraBuffer(droppedItems: ArrayBuffer[ItemStack]) {
    def add(drops: java.util.Collection[org.bukkit.inventory.ItemStack]): ArrayBuffer[ItemStack] = {
      droppedItems ++= drops.asScala
    }
  }
  def detonate(loc: Location, grenade: Grenade, blocks: Array[Block]): Array[ItemStack] = {
    this.detonationLoc=loc
    this.grenade=grenade
    this.dropItems=grenade.explosion.dropItems
    this.damage=grenade.explosion.damage
    this.power=grenade.explosion.power.toDouble
    this.explosionExtra=grenade.explosion.extra
    detonate(loc: Location, blocks: Array[Block])
    cleanup()
    this.droppedItems.toArray
  }
}
