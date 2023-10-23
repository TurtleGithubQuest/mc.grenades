package dev.turtle.grenades
package utils.parts

import Main.{debugMode, decimalFormat, plugin}
import utils.Conf.cConfig
import utils.{Blocks, Grenade}

import dev.turtle.grenades.utils.lang.Message.{debugMessage, sendMessage}
import org.bukkit.Particle.DustOptions
import org.bukkit.block.Block
import org.bukkit.command.CommandSender
import org.bukkit.entity.AbstractArrow.PickupStatus
import org.bukkit.{Bukkit, ChatColor, Location, Material, NamespacedKey, Particle, World}
import org.bukkit.entity.{ArmorStand, Arrow, Entity, EntityType, Item, Player, Snowball, TNTPrimed}
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.{BukkitRunnable, BukkitTask}

import scala.jdk.CollectionConverters.*

class GrenadeEntity(val grenade: Grenade,
                    val owner: Player = null
                  ) {
  var gModel: Entity = null
  var selfDestruct: Boolean = false

  def spawn(loc: Location, direction: org.bukkit.util.Vector): Boolean = {
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
        debugMessage(Bukkit.getConsoleSender, s"&8(${grenade.displayName}&8) &cCouldn't spawn &6'${grenade.model}'&c, entity not found!", Map())
        selfDestruct = true
        return false
    }
    gModel.setCustomName(grenade.customName.replaceAll("%countdown%", (grenade.fuseTime/20).toString))
    gModel.setGlowing(grenade.glow)
    gModel.setInvulnerable(true)
    gModel.setCustomNameVisible(grenade.customNameVisible)
    countdown.start()
    position.sync()
    particle.start()
    true
  }

  def detonate(): Boolean = {
    val gModelLocation = gModel.getLocation
    val blocks: Array[Block] = Blocks.getInRadius(gModelLocation, grenade.explosion.power)
    val originName = {
      if (owner ne null) owner.getName
      else "unknown"
    }
    gModelLocation.getWorld.playSound(gModelLocation, grenade.explosion.sound.name, grenade.explosion.sound.volume, grenade.explosion.sound.pitch)
    val entityExplodeEvent = new EntityExplodeEvent(gModel, gModelLocation, blocks.toList.asJava, grenade.explosion.power.toFloat)
    plugin.getServer.getPluginManager.callEvent(entityExplodeEvent)
    if (!entityExplodeEvent.isCancelled)
      grenade.explosion.name.detonate(gModelLocation, blocks, originName=originName, params=grenade.explosion.extra)
    true
  }
  private object countdown {
    def start(): BukkitTask = {
      new BukkitRunnable() {
        var detonationTime: Double = grenade.fuseTime.toDouble
        override def run(): Unit = {
          if (detonationTime == 0 || selfDestruct || !gModel.isValid ) {
            detonate()
            if (gModel.isValid) gModel.remove()
            cancel()
          }
          if (gModel.isOnGround || gModel.getLocation.getBlock.isLiquid) {
            if (grenade.customNameVisible) {
              //gModel.setCustomName(s"${ChatColor.RED}${ChatColor.BOLD}${decimalFormat.format(detonationTime / 20)}s")
              val countdown = decimalFormat.format(detonationTime / 20)
              gModel.setCustomName(grenade.customName.replaceAll("%countdown%", countdown))
              /*gModel.setCustomName(
                ChatColor.translateAlternateColorCodes('&',
                grenade.customName.replaceAll("%countdown%", countdown)
                ))*/
            }
            detonationTime -= 1
          }
        }
      }.runTaskTimer(plugin, 0L, 1L)
    }
  }

  private object position {
    def sync(): BukkitTask = {
      new BukkitRunnable() {
        override def run(): Unit = {
          if (selfDestruct || !gModel.isValid) cancel()
        }
      }.runTaskTimer(plugin, 0L, cConfig.getInt("general.refresh-rate"))
    }
  }

  private object particle {
    def start(): BukkitTask = {
      new BukkitRunnable() {
        override def run(): Unit = {
          if (selfDestruct || !gModel.isValid) cancel()
          if (!gModel.isOnGround)
            grenade.trail.name match
              case "REDSTONE" =>
                val dustOptions = new DustOptions(grenade.trail.color, (grenade.trail.size).toFloat)
                gModel.getWorld.spawnParticle(Particle.REDSTONE, gModel.getLocation, grenade.trail.amount, grenade.trail.offset("x"), grenade.trail.offset("y"), grenade.trail.offset("z"), dustOptions)
              case _ =>
                gModel.getWorld.spawnParticle(Particle.valueOf(grenade.trail.name), gModel.getLocation, grenade.trail.amount, grenade.trail.offset("x"), grenade.trail.offset("y"), grenade.trail.offset("z"))
        }
      }.runTaskTimer(plugin, 3L, 1L)
    }
  }
}
