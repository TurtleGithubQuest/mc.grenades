package dev.turtle.grenades
package utils.extras

import enums.DropLocation
import enums.DropLocation.*

import org.bukkit.Location
import org.bukkit.entity.{LivingEntity, Player}
import org.bukkit.inventory.{BlockInventoryHolder, InventoryHolder, ItemStack}

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
  }

