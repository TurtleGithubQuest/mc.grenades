package dev.turtle.grenades
package utils.parts

import Main.{decimalFormat, plugin}
import utils.Grenade
import utils.lang.Message.debugMessage

import org.bukkit.entity.*
import org.bukkit.entity.AbstractArrow.PickupStatus
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.{BukkitRunnable, BukkitTask}
import org.bukkit.{Bukkit, Location, World}

import scala.jdk.CollectionConverters.*
import org.bukkit.util.Vector as BukkitVector

class GrenadeEntity(val grenade: Grenade,
                    val owner: Player = null
                  ) {
  var gModel: Entity = null
  var selfDestruct: Boolean = false

  def spawn(loc: Location, direction: org.bukkit.util.Vector): GrenadeEntity = {
    val world: World = loc.getWorld

    loc.setY(loc.getY+1)
    val velocity = direction.multiply(grenade.velocity)
    try {
      gModel = grenade.model match {
        case "TNT" | "PRIMED_TNT" =>
          world.spawn(loc, classOf[TNTPrimed], { (tnt: TNTPrimed) =>
            tnt.setVelocity(velocity.multiply(1.1))
            tnt.setYield(0)
            tnt.setFuseTicks(42069)
            tnt.setSilent(true)
            tnt.setSource(owner)
          })
        case "SNOWBALL" =>
          world.spawn(loc, classOf[Snowball], { (snowball: Snowball) =>
            snowball.setVelocity(velocity)
            snowball.setShooter(owner)
          })
        case "ARROW" =>
          world.spawn(loc, classOf[Arrow], { (arrow: Arrow) =>
            arrow.setVelocity(velocity)
            arrow.setDamage(0)
            arrow.setPierceLevel(100)
            arrow.setPersistent(true)
            arrow.setShooter(owner)
            arrow.setPickupStatus(PickupStatus.DISALLOWED)
          })
        case "ITEM" | "DROPPED_ITEM" =>
          var itemStack = new ItemStack(grenade.material)
          itemStack.setAmount(1)
          world.dropItem(loc, itemStack, { (item: Item) =>
            item.setVelocity(velocity)
            item.setPickupDelay(42069)
            item.setCustomName(grenade.customName)
          }).asInstanceOf[Entity]
        case _ =>
            world.spawn(loc, EntityType.valueOf(grenade.model).getEntityClass, { (entity: Entity) =>
              entity.setVelocity(velocity)
              entity.setCustomName(grenade.customName)
            })
      }
    } catch {
      case e: IllegalArgumentException =>
        debugMessage(s"&8(${grenade.displayName}&8) &cCouldn't spawn &6'${grenade.model}'&c, entity not found!", Map())
        selfDestruct = true
        return null
    }
    gModel.setCustomName(grenade.customName.replaceAll("%countdown%", (grenade.fuseTime/20).toString))
    gModel.setGlowing(grenade.glow)
    gModel.setInvulnerable(true)
    gModel.setCustomNameVisible(grenade.customNameVisible)
    countdown.start()
    particle.start()
    this
  }

  def detonate(): Boolean = {
    val gModelLocation = gModel.getLocation
    val originName = {
      if (owner ne null) owner.getName
      else "unknown"
    }
    grenade.explosion.spawnAt(gModelLocation, source=owner, grenadeEntity=gModel)
    true
  }
  private object countdown {
    def start(): BukkitTask = {
      new BukkitRunnable() {
        var detonationTime: Double = grenade.fuseTime.toDouble
        var gModelVector: org.bukkit.util.Vector = gModel.getVelocity
        override def run(): Unit = {
          if (detonationTime == 0 || selfDestruct || !gModel.isValid ) {
            detonate()
            if (gModel.isValid) gModel.remove()
            cancel()
          }
          if (gModel.isOnGround || gModel.getLocation.getBlock.isLiquid) {
            if (grenade.customNameVisible) {
              val countdown = decimalFormat.format(detonationTime / 20)
              gModel.setCustomName(grenade.customName.replaceAll("%countdown%", countdown))
            }
            detonationTime -= 1
          }
          if (grenade.ricochet) {
            val impactVector = gModel.getVelocity.normalize.subtract(gModelVector.normalize)

            if ((impactVector.length() > 1) && (impactVector.length() < 2)) { //Impact
              val normal = new BukkitVector(impactVector.getX, impactVector.getY, impactVector.getZ)
              gModel.setVelocity(
                gModelVector.subtract(
                  normal.multiply(
                    2*gModelVector.dot(normal)
                  )
                )
              )
            }
          }
        }
      }.runTaskTimer(plugin, 0L, 1L)
    }
  }
  private object particle {
    def start(): BukkitTask = {
      new BukkitRunnable() {
        override def run(): Unit = {
          if (selfDestruct || !gModel.isValid) cancel()
          if (!gModel.isOnGround)
            grenade.trail.spawnAt(gModel.getLocation)
        }
      }.runTaskTimer(plugin, 3L, 1L)
    }
  }
}
