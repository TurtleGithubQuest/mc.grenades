package dev.turtle.grenades
package utils

import dev.turtle.grenades.utils.Conf.{getFolder, grenades, landmines, reloadLandmines}
import org.bukkit.{Bukkit, Location}

object Landmine {
  def isPresent(loc: Location, detonate: Boolean = true): Boolean = {
    val world = loc.getWorld
    if (!landmines.hasPath(world.getName)) {return false}
    val chunk = loc.getChunk
    val chunkPath = s"${chunk.getX}/${chunk.getZ}"
    val worldName = world.getName
    var cWorld = landmines.getConfig(worldName)
    if (!cWorld.hasPath(chunkPath)) {return false}
    val landmineCoords = s"$chunkPath.${loc.getX.toInt}/${loc.getY.toInt}/${loc.getZ.toInt}"
    if (cWorld.hasPath(landmineCoords)) {
      if (!detonate) {return true}
      val grenade_id = cWorld.getString(s"$landmineCoords.grenade_id")
      val grenade_owner = cWorld.getString(s"$landmineCoords.owner")
      val grenade = grenades(grenade_id)
      val success = grenade.spawn(loc, org.bukkit.util.Vector(0, 0, 0), Bukkit.getPlayer(grenade_owner))
      if (success) {
        cWorld = Conf.setValue(cWorld, s"$landmineCoords", "")
        Conf.save(cWorld, s"${getFolder("landmines")}/${worldName}.json")
        reloadLandmines()
      }
    }
    true
  }
}
