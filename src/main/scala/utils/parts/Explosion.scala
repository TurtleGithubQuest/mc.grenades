package dev.turtle.grenades
package utils.parts

import Main.plugin
import enums.DropLocation
import explosions.base.ExplosionType
import utils.Blocks
import utils.Blocks.ShrimpleBlock

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.{Damageable, Entity}
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.inventory.InventoryHolder

import java.lang.Class.forName
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}


case class Explosion(
                      explosionType: ExplosionType,
                      particles: Particle,
                      power: Integer,
                      damage: Double,
                      shape: String,
                      sound: Sound,
                      var source: InventoryHolder=null
                    ) {
  def cleanup(loc: Location, blocks: Array[ShrimpleBlock]): Boolean = {
    Blocks.addToQueue(loc.getWorld, blocks)
    val nearbyEntities = loc.getWorld.getNearbyEntities(loc, power.toDouble, power.toDouble, power.toDouble)
    for (entity <- nearbyEntities.asScala) {
      if ((entity ne null) && entity.isInstanceOf[Damageable])
        entity.asInstanceOf[Damageable].damage(damage)
    }
    true
  }
  def spawnAt(loc: Location, source: InventoryHolder=null, grenadeEntity: Entity = null): Boolean = {
    val blocksInRadiusAsync: Future[Array[Block]] = Blocks.getInRadius(loc, power)
    this.source=source
    sound.playAt(loc)
    particles.spawnAt(loc)
    val result: Try[Array[Block]] = Await.ready(blocksInRadiusAsync, Duration.Inf).value.get
    result match {
      case Success(blocks) =>
        val entityExplodeEvent = new EntityExplodeEvent(grenadeEntity, loc, blocks.toList.asJava, power.toFloat)
        plugin.getServer.getPluginManager.callEvent(entityExplodeEvent)
        if (!entityExplodeEvent.isCancelled) {
          cleanup(loc, explosionType.filterBlocks(loc=loc, blocks=blocks, source=source))
          true
        } else false
      case Failure(f) =>
        false
    }
  }
}