package dev.turtle.grenades
package listeners.bukkit

import Main.{cooldown, decimalFormat}
import utils.Blocks.canDestroyThatBlock
import utils.Conf._
import utils.Landmine.coordsFromLoc
import utils.extras.ExtraCommandSender
import utils.{Conf, Grenade, Landmine}

import com.typesafe.config.ConfigValueFactory
import de.tr7zw.changeme.nbtapi.NBTItem
import dev.turtle.grenades.listeners.base.ExtraListener
import net.md_5.bungee.api.ChatMessageType
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.{EventHandler, EventPriority}
import org.bukkit.inventory.ItemStack

import scala.collection.immutable
import scala.jdk.CollectionConverters.*

class InteractEvent extends ExtraListener with ExtraCommandSender {
  private val config = configs("config")
  private val CooldownTime = config.getInt("cooldown.time") / 20 * 1000
  private val IsCooldownNotifyEnabled = config.getBoolean("cooldown.notify.enabled")
  private val IsLandmineEnabled = config.getBoolean("landmine.enabled")

  @EventHandler(priority = EventPriority.HIGH)
  private def interact(e: PlayerInteractEvent): Unit = {
    for {
      itemInHand <- Option(e.getItem) if isValidInteraction(itemInHand, e)
      grenadeId <- getGrenadeId(itemInHand)
      grenade <- grenades.get(grenadeId)
    } yield {
      e.setCancelled(true)
      handleGrenadeUsage(e.getPlayer, itemInHand, grenade, e)
    }
  }

  private def isValidInteraction(item: ItemStack, e: PlayerInteractEvent): Boolean =
    item.getType != Material.AIR && e.getAction.toString.contains("RIGHT_CLICK")

  private def getGrenadeId(item: ItemStack): Option[String] = {
    val nbtItem = new NBTItem(item)
    Option.when(nbtItem.hasTag("grenade_id"))(nbtItem.getString("grenade_id"))
  }

  private def handleGrenadeUsage(player: Player, item: ItemStack, grenade: Grenade, e: PlayerInteractEvent): Unit = {
    if (checkCooldown(player)) {
      val success = if (grenade.isLandmine) handleLandmine(player, grenade.id, e)
                   else handleGrenade(player, grenade)

      if (success) reduceItemAmount(item)
    }
    cooldown.put(player.getName, System.currentTimeMillis)
  }

  private def checkCooldown(player: Player): Boolean = {
    val remainingTime = System.currentTimeMillis - cooldown(player.getName)
    if (remainingTime > 0 && remainingTime < CooldownTime && !player.hasPermission("grenades.cooldown.bypass")) {
      notifyCooldown(player, remainingTime)
      false
    } else true
  }

  private def notifyCooldown(player: Player, remainingTime: Long): Unit = {
    if (IsCooldownNotifyEnabled) {
      val cooldownInSeconds = (CooldownTime - remainingTime) / 1000.0
      val messageType = if (config.getString("cooldown.notify.medium").equalsIgnoreCase("chat"))
                         ChatMessageType.CHAT else ChatMessageType.ACTION_BAR
      player.sendMessage("cooldown.notify",
        Map("cooldown" -> decimalFormat.format(cooldownInSeconds)),
        chatMessageType = messageType)
    }
  }

  private def handleLandmine(player: Player, grenadeId: String, e: PlayerInteractEvent): Boolean = {
    if (!IsLandmineEnabled) return false

    Option(e.getClickedBlock).filter(_.getType != Material.AIR) match {
      case Some(block) if canDestroyThatBlock(player, block) =>
        placeLandmine(player, block, grenadeId)
        true
      case Some(_) =>
        player.sendMessage("landmine.placement.no-perm", Map())
        false
      case None =>
        player.sendMessage("landmine.placement.invalid", Map())
        false
    }
  }

  private def placeLandmine(player: Player, block: Block, grenadeId: String): Unit = {
    val worldName = block.getWorld.getName
    val landmineCoords = coordsFromLoc(block.getLocation)

    if (!landmines.hasPath(worldName)) {
      landmines = landmines.withValue(worldName, ConfigValueFactory.fromMap(Map().asJava))
    }

    Landmine.saveAndReloadAll(worldName, immutable.Map(
      s"$landmineCoords.owner" -> player.getName,
      s"$landmineCoords.grenade_id" -> grenadeId
    ))
    block.setType(Material.OAK_PRESSURE_PLATE)
  }

  private def handleGrenade(player: Player, grenade: Grenade): Boolean =
    grenade.spawn(player.getLocation, player.getLocation.getDirection, owner = player) != null

  private def reduceItemAmount(item: ItemStack): Unit =
    item.setAmount(item.getAmount - 1)
}