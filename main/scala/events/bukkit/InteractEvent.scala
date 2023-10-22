package dev.turtle.grenades
package events.bukkit

import Main.{cooldown, decimalFormat}
import utils.Blocks.canDestroyThatBlock
import utils.Conf.{cConfig, grenades}
import utils.Grenade

import de.tr7zw.changeme.nbtapi.NBTItem
import dev.turtle.grenades.utils.lang.Message.sendMessage
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.block.Block
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.{EventHandler, EventPriority, Listener}
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.{Bukkit, GameMode, Material}

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
    if (grenade.isLandmine) {
      val block: Block = e.getClickedBlock
      if (
        block.getType.equals(Material.AIR) ||
          !canDestroyThatBlock(p, block) ||
          !cConfig.getBoolean("landmine.enabled")
      ) return
      //TODO: Add landmines

    } else {
      grenade.spawn(p.getLocation, p.getLocation.getDirection, owner = p)
    }
    itemInHand.setAmount(itemInHand.getAmount - 1)
  }
}
