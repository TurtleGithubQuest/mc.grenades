package dev.turtle.grenades
package utils.extras

import enums.DropLocation
import enums.DropLocation.*

import de.tr7zw.changeme.nbtapi.NBTItem
import dev.turtle.grenades.utils.lang.Message.{placeholderPrefix, textPrefix}
import org.bukkit.Location
import org.bukkit.entity.{LivingEntity, Player}
import org.bukkit.inventory.{BlockInventoryHolder, InventoryHolder, ItemStack}

import scala.collection.immutable
import scala.jdk.CollectionConverters.*
import java.util.List as JavaList

  implicit class ExtraItemStack(itemStack: ItemStack) {
    def drop(dropLocations: Array[DropLocation], blockLoc: Location=null, inventoryHolder: InventoryHolder=null): Boolean = {
      var itemDropped = false
      for (dropLoc: DropLocation <- dropLocations) {
        if (!itemDropped)
          dropLoc match {
            case BlockLocation =>
              blockLoc.getWorld.dropItem(blockLoc, itemStack)
              itemDropped = true
            case InventoryOfSource =>
              if ((inventoryHolder ne null) && (inventoryHolder.getInventory.firstEmpty > 0)) {
                inventoryHolder.getInventory.addItem(itemStack)
                itemDropped = true
              }
            case SourceLocation =>
              if (inventoryHolder ne null) {
                val loc: Location = inventoryHolder match {
                  case le: LivingEntity =>
                    le.getLocation
                  case bih: BlockInventoryHolder =>
                    bih.getBlock.getLocation
                }
                if (loc ne null) {
                  loc.getWorld.dropItem(loc, itemStack)
                  itemDropped = true
                }
              }
          }
      }
      itemDropped
    }
    def updateLore(placeholders: immutable.Map[String, Any]): Boolean = {
      var itemMeta = itemStack.getItemMeta
      var itemLore: JavaList[String] = itemMeta.getLore
      for ((key, value) <- placeholders) {
        if (itemLore.toString.contains(s"%$key%")){
          value match
            case valueList: JavaList[String] => itemLore = valueList
            case _ => itemLore = itemLore.asScala.map(_.replaceAll(s"%$key%", value.toString)).asJava
        }
      }
      itemMeta.setLore(itemLore.asScala.map(_.replaceAll("&", "§")).asJava)
      itemStack.setItemMeta(itemMeta)
      true
    }
    def removeLore(): Unit = {
      val itemMeta = itemStack.getItemMeta
      itemMeta.setLore(new java.util.ArrayList[String])
      itemStack.setItemMeta(itemMeta)
    }
    def isGrenade: Boolean = NBTItem(itemStack).hasTag("grenade_id")
    def getGrenadeID: String = NBTItem(itemStack).getString("grenade_id")

  }

