package dev.turtle.grenades
package explosions

import Main.faweapi
import utils.Blocks.setBlockType
import utils.parts.ExplosionType

import com.fastasyncworldedit.core.FaweAPI
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.world.block.BlockTypes
import com.sk89q.worldedit.{EditSession, WorldEdit}
import org.bukkit.block.Block
import org.bukkit.{Location, Material}

object Classic extends ExplosionType {
  override def detonate(loc: Location, blocks: Array[Block], originName: String, params: String): Boolean = {
    if (faweapi != null) {
     /* var editSession: EditSession = WorldEdit.getInstance().newEditSessionBuilder().world(FaweAPI.getWorld(loc.getWorld.getName)).fastMode(true).build()
      val airBlock = BlockTypes.AIR.getDefaultState
      for (block: Block <- blocks) {
        val blockVector3 = BlockVector3.at(block.getX, block.getY, block.getZ)
        editSession.smartSetBlock(blockVector3, airBlock)
      }
      editSession.flushQueue()*/
    } else {
      for (block: Block <- blocks) {
        setBlockType(block, Material.AIR, true, originName = originName)
      }
    }
    true
  }
}
