package dev.turtle.grenades
package utils.parts

import Main.{decimalFormat, plugin}
import utils.Conf.cConfig
import utils.{Blocks, Grenade}
import org.bukkit.Particle.DustOptions
import org.bukkit.block.Block
import org.bukkit.entity.AbstractArrow.PickupStatus
import org.bukkit.{Bukkit, ChatColor, Location, Material, NamespacedKey, Particle, World}
import org.bukkit.entity.{ArmorStand, Arrow, Entity, Item, Player, Snowball, TNTPrimed}
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.{BukkitRunnable, BukkitTask}

import scala.jdk.CollectionConverters.*

class GrenadeEntity(val grenade: Grenade,
                    val owner: Player = null
                  ) {
  var armorStand: ArmorStand = null
  var gModel: Entity = null
  var selfDestruct: Boolean = false

  def spawn(loc: Location, direction: org.bukkit.util.Vector): Boolean = {
    val world: World = loc.getWorld
    armorStand = world.spawn(loc, classOf[ArmorStand], { (as: ArmorStand) =>
      as.setVisible(false)
      as.setSmall(true)
      as.setSilent(true)
      as.setInvulnerable(true)
      as.setVelocity(direction.multiply(grenade.velocity))
      as.getPersistentDataContainer.set(new NamespacedKey(Main.plugin, "grenade_id"), PersistentDataType.STRING, grenade.id);
    })

    gModel = grenade.model match {
      case "TNT" =>
        world.spawn(loc, classOf[TNTPrimed], { (tnt: TNTPrimed) =>
          tnt.setVelocity(armorStand.getVelocity.multiply(1.1))
          tnt.setYield(0)
          tnt.setFuseTicks(42069)
          tnt.setSilent(true)
          tnt.setSource(owner)
        })
      case "SNOWBALL" =>
        world.spawn(loc, classOf[Snowball], { (snowball: Snowball) =>
          snowball.setVelocity(armorStand.getVelocity)
          snowball.setShooter(owner)
        })
      case "ARROW" =>
        world.spawn(loc, classOf[Arrow], { (arrow: Arrow) =>
          arrow.setVelocity(armorStand.getVelocity)
          arrow.setDamage(0)
          arrow.setPersistent(true)
          arrow.setShooter(owner)
          arrow.setPickupStatus(PickupStatus.DISALLOWED)
        })
      case _ =>
        var itemStack = new ItemStack(grenade.material)
        itemStack.setAmount(1)
        world.dropItem(loc, itemStack, { (item: Item) =>
          item.setVelocity(armorStand.getVelocity)
          item.setPickupDelay(42069)
        }).asInstanceOf[Entity]
    }
    gModel.setGlowing(grenade.glow)
    gModel.setInvulnerable(true)
    gModel.setCustomNameVisible(true)
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
    grenade.explosion.name.detonate(gModelLocation, blocks, originName=originName, params=grenade.explosion.extra)
    plugin.getServer.getPluginManager.callEvent(
      new EntityExplodeEvent(gModel, gModelLocation, blocks.toList.asJava, grenade.explosion.power.toFloat)
    )
    true
  }
  private object countdown {
    def start(): BukkitTask = {
      new BukkitRunnable() {
        var detonationTime: Double = grenade.countdownTime.toDouble

        override def run(): Unit = {
          if (detonationTime == 0 || !gModel.isValid || !armorStand.isValid || selfDestruct) {
            detonate()
            if (gModel.isValid) gModel.remove()
            if (armorStand.isValid) armorStand.remove()
            cancel()
          }
          if (gModel.isOnGround || gModel.getLocation.getBlock.isLiquid) {
            if (grenade.countdownVisible) {
              gModel.setCustomName(s"${ChatColor.RED}${ChatColor.BOLD}${decimalFormat.format(detonationTime / 20)}s")
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
          if (!armorStand.isValid || selfDestruct) cancel()
          if (gModel.isValid) armorStand.teleport(gModel.getLocation)
        }
      }.runTaskTimer(plugin, 0L, cConfig.getInt("general.refresh-rate"))
    }
  }

  private object particle {
    def start(): BukkitTask = {
      new BukkitRunnable() {
        override def run(): Unit = {
          if (!armorStand.isValid || selfDestruct) cancel()
          if (!gModel.isOnGround)
            grenade.trail.name match
              case "REDSTONE" =>
                val dustOptions = new DustOptions(grenade.trail.color, (grenade.trail.size).toFloat)
                armorStand.getWorld.spawnParticle(Particle.REDSTONE, armorStand.getLocation, grenade.trail.amount, grenade.trail.offset("x"), grenade.trail.offset("y"), grenade.trail.offset("z"), dustOptions)
              case _ =>
                armorStand.getWorld.spawnParticle(Particle.valueOf(grenade.trail.name), armorStand.getLocation, grenade.trail.amount, grenade.trail.offset("x"), grenade.trail.offset("y"), grenade.trail.offset("z"))
        }
      }.runTaskTimer(plugin, 3L, 1L)
    }
  }
}
