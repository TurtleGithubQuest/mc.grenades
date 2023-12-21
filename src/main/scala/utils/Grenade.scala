package dev.turtle.grenades

package utils

import utils.parts.{Explosion, GrenadeEntity, Particle}

import de.tr7zw.changeme.nbtapi.NBTItem
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.{ChatColor, Location, Material}

import java.util

case class Grenade(
             id: String,
             displayName: String,
             lore: util.ArrayList[String],
             explosion: Explosion,
             isLandmine: Boolean,
             trail: Particle,
             material: Material,
             glow: Boolean,
             model: String,
             customNameVisible: Boolean,
             customName: String,
             fuseTime: Integer,
             velocity: Double,
             customModelData: Integer,
             ricochet: Boolean
             ) {

      def apply: Boolean = {
        val item: NBTItem = new NBTItem(new ItemStack(material))
        val fields = this.getClass.getDeclaredFields
        fields.foreach { field =>
          field.setAccessible(true)
          item.setString(
            field.getName,
            (field.get(this)).toString
          )
        }
        true
      }

      def spawn(loc: Location, direction: org.bukkit.util.Vector, owner: Player = null): GrenadeEntity = {
        val grenadeEntity = new GrenadeEntity(this, owner)
        grenadeEntity.spawn(loc, direction)
      }

      def item: ItemStack = {
        var itemStack: ItemStack = new ItemStack(material)
        var nbtItem = new NBTItem(itemStack)
        nbtItem.setString("grenade_id", this.id)
        nbtItem.mergeNBT(itemStack)
        val itemMeta = itemStack.getItemMeta
        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', this.displayName))
        itemMeta.setLore(this.lore)
        itemMeta.setCustomModelData(this.customModelData)
        itemStack.setItemMeta(itemMeta)
        itemStack
      }
}