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

class InteractEvent extends ExtraListener, ExtraCommandSender {

  @EventHandler(priority = EventPriority.HIGH)
  private def interact(e: PlayerInteractEvent): Unit = {
    val itemInHand: ItemStack = e.getItem
    if (itemInHand == null ||
        itemInHand.getType == Material.AIR ||
        !e.getAction.toString.contains("RIGHT_CLICK")
    ) return
    val nbtItem: NBTItem = new NBTItem(itemInHand)
    if (!nbtItem.hasTag("grenade_id")) return
    e.setCancelled(true)
    val grenade_id: String = nbtItem.getString("grenade_id")
    if (!grenades.contains(grenade_id)) return //TODO: Take item from player and log it
    val p: Player = e.getPlayer
    val timeNow: Long = System.currentTimeMillis
    val cooldownRemaining: Long = (timeNow - cooldown(p.getName))
    val cooldownThreshold = configs("config").getInt("cooldown.time") / 20 * 1000
    if (cooldownRemaining > 0 && cooldownRemaining < cooldownThreshold) {
      if (!p.hasPermission("grenades.cooldown.bypass")) {
        val cooldownInMs: Double = (cooldownThreshold - cooldownRemaining) / 100
        if (configs("config").getBoolean("cooldown.notify.enabled"))
          p.sendMessage("cooldown.notify", Map(
            "cooldown" -> decimalFormat.format(cooldownInMs / 10)),
            chatMessageType = {
              if (configs("config").getString("cooldown.notify.medium").equalsIgnoreCase("chat"))
                ChatMessageType.CHAT
              else ChatMessageType.ACTION_BAR
            }
          )
          return
      }
    }
    cooldown.put(p.getName, timeNow)
    val grenade: Grenade = grenades(grenade_id)
    var success: Boolean = true
    if (grenade.isLandmine) {
      val block: Block = e.getClickedBlock
      if (!configs("config").getBoolean("landmine.enabled")) {}
      else if (block == null ||
          block.getType.equals(Material.AIR)
      ) p.sendMessage("landmine.placement.invalid", Map())
      else if (!canDestroyThatBlock(p, block))
        p.sendMessage("landmine.placement.no-perm", Map())
      else {
        val worldName: String = block.getWorld.getName
        val landmineCoords = Landmine.coordsFromLoc(e.getClickedBlock.getLocation)
        if (!landmines.hasPath(worldName))
          landmines = landmines.withValue(worldName, ConfigValueFactory.fromMap(Map().asJava))
        Landmine.saveAndReloadAll(worldName, immutable.Map(
          s"$landmineCoords.owner" -> p.getName,
          s"$landmineCoords.grenade_id" -> grenade_id
        ))
        block.setType(Material.OAK_PRESSURE_PLATE)
      }
    } else {
      success = (grenade.spawn(p.getLocation, p.getLocation.getDirection, owner = p) ne null)
    }
    if (success)
      itemInHand.setAmount(itemInHand.getAmount - 1)
  }
}
