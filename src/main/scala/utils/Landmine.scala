package dev.turtle.grenades
package utils

import utils.Conf.{grenades, landmines, reloadConfigsInFolder}
import utils.extras.ExtraConfig

import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import org.bukkit.{Bukkit, Chunk, Location}

import scala.collection.immutable

object Landmine {
  def isPresent(loc: Location, detonate: Boolean = true): Boolean = {
    val world = loc.getWorld
    var isPresent: Boolean = false
    if (landmines.hasPath(world.getName)) {
      val chunk = loc.getChunk
      val chunkPath = s"${chunk.getX}/${chunk.getZ}"
      val worldName = world.getName
      var cWorld = landmines.getConfig(worldName)
      if (cWorld.hasPath(chunkPath)) {
        val landmineCoords = s"$chunkPath.${loc.getX.toInt}/${loc.getY.toInt}/${loc.getZ.toInt}"
        if (cWorld.hasPath(landmineCoords)) {
          if (detonate) {
            val grenade_id = cWorld.getString(s"$landmineCoords.grenade_id")
            val grenade_owner = cWorld.getString(s"$landmineCoords.owner")
            val grenade = grenades(grenade_id)
            Landmine.saveAndReloadAll(worldName, immutable.Map(landmineCoords -> ""))
            grenade.explosion.spawnAt(loc, source=Bukkit.getPlayer(grenade_owner))
          }
          isPresent = true
        }
      }
    }

    isPresent
  }

  def saveAndReloadAll(worldName: String, keysAndValues: immutable.Map[String, String]): Boolean = {
    val updatedWorldConfig = keysAndValues.foldLeft(landmines.getConfig(worldName)) {
      case (config, (key, value)) if value.isEmpty =>
        config.withoutPath(key)
      case (config, (key, value)) =>
        config.withValue(key, ConfigValueFactory.fromAnyRef(value))
    }
    updatedWorldConfig.save(path=s"data/landmines/$worldName.json")
    landmines = ConfigFactory.empty()
    reloadConfigsInFolder(folderPath = "data/landmines")
    true
  }

  def coordsFromLoc(loc: Location, chunk: Option[Chunk] = None): String = {
    val targetChunk = chunk.getOrElse(loc.getChunk)
    s"${targetChunk.getX}/${targetChunk.getZ}.${loc.getX.toInt}/${loc.getY.toInt}/${loc.getZ.toInt}"
  }
}
