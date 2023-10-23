package dev.turtle.grenades
package events.bukkit

import Main.{cooldown, decimalFormat}
import utils.Blocks.canDestroyThatBlock
import utils.Conf.*
import utils.lang.Message.sendMessage
import utils.{Conf, Grenade}

import com.typesafe.config.{Config, ConfigValueFactory}
import de.tr7zw.changeme.nbtapi.NBTItem
import net.md_5.bungee.api.ChatMessageType
import org.bukkit.block.Block
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.{EventHandler, EventPriority, Listener}
import org.bukkit.inventory.ItemStack
import org.bukkit.{Location, Material}

import scala.jdk.CollectionConverters.*

class InteractEvent extends Listener{

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
    val cooldownThreshold = cConfig.getInt("cooldown.time") / 20 * 1000
    if (cooldownRemaining > 0 && cooldownRemaining < cooldownThreshold) {
      if (!p.hasPermission("grenades.cooldown.bypass")) {
        val cooldownInMs: Double = (cooldownThreshold - cooldownRemaining) / 100
        if (cConfig.getBoolean("cooldown.notify.enabled"))
          sendMessage(p.asInstanceOf[CommandSender], "cooldown.notify", Map(
            "cooldown" -> decimalFormat.format(cooldownInMs / 10)),
            chatMessageType = {
              if (cConfig.getString("cooldown.notify.medium").equalsIgnoreCase("chat"))
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
      if (block == null ||
          block.getType.equals(Material.AIR) ||
          !canDestroyThatBlock(p, block) ||
          !cConfig.getBoolean("landmine.enabled")
      ) return
      val worldName: String = block.getWorld.getName
      val loc: Location = e.getClickedBlock.getLocation
      val landmineCoords = s"${loc.getChunk.getX}/${loc.getChunk.getZ}.${loc.getX.toInt}/${loc.getY.toInt}/${loc.getZ.toInt}"
      if (!landmines.hasPath(worldName))
        landmines = landmines.withValue(worldName, ConfigValueFactory.fromMap(Map().asJava))
      var worldConf: Config = landmines.getConfig(worldName)
      worldConf = Conf.setValue(worldConf, s"$landmineCoords.owner", p.getName)
      worldConf = Conf.setValue(worldConf, s"$landmineCoords.grenade_id", grenade_id)
      Conf.save(worldConf, s"${getFolder("landmines")}/${worldName}.json")
      reloadLandmines()
      block.setType(Material.OAK_PRESSURE_PLATE)
    } else {
      success = grenade.spawn(p.getLocation, p.getLocation.getDirection, owner = p)
    }
    if (success)
      itemInHand.setAmount(itemInHand.getAmount - 1)
  }
}
