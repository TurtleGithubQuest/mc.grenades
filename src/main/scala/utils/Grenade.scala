package dev.turtle.grenades

package utils

import de.tr7zw.changeme.nbtapi.NBTItem
import utils.parts.{Explosion, GrenadeEntity, gParticle}
import utils.Blocks

import dev.turtle.grenades.Main.plugin
import dev.turtle.grenades.utils.Conf.cConfig
import org.bukkit.Particle.DustOptions
import org.bukkit.block.Block
import org.bukkit.{ChatColor, Location, Material, Particle, World}
import org.bukkit.entity.{ArmorStand, Arrow, Entity, EntityType, Item, Player, Snowball, TNTPrimed}
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.scheduler.{BukkitRunnable, BukkitTask}

import java.awt.Color
import java.text.DecimalFormat
import java.util


trait T_Grenade {
  def id: String
  def displayName: String
  def lore: util.ArrayList[String]
  def explosion: Explosion
  def isLandmine: Boolean
  def trail: gParticle
  def material: Material
  //def armorStand: ArmorStand
  def glow: Boolean
  def model: String
  def customNameVisible: Boolean
  def customName: String
  def fuseTime: Integer
  def velocity: Double
}
class Grenade(
             val id: String,
             val displayName: String,
             val lore: util.ArrayList[String],
             val explosion: Explosion,
             val isLandmine: Boolean,
             val trail: gParticle,
             val material: Material,
             val glow: Boolean,
             val model: String,
             val customNameVisible: Boolean,
             val customName: String,
             val fuseTime: Integer,
             val velocity: Double
             ) extends T_Grenade {

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

      def spawn(loc: Location, direction: org.bukkit.util.Vector, owner: Player = null): Boolean = {
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
        itemStack.setItemMeta(itemMeta)
        itemStack
      }
}