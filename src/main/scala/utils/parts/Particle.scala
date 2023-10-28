package dev.turtle.grenades

package utils.parts

import org.bukkit.Particle.DustOptions
import org.bukkit.{Color, Location}

import scala.collection.mutable

case class Particle(
              name: String,
              amount: Integer,
              size: Float,
              colorHEX: String,
              color: Color,
              colorFade: Color,
              offset: mutable.Map[String, Double] = mutable.Map("x" -> 0, "y" -> 0, "z" -> 0)
              ) {
  def spawnAt(loc: Location): Boolean = {
    var dustOptions: DustOptions = null
    val particle = name match {
      case "REDSTONE" =>
        dustOptions = new DustOptions(color, size)
        org.bukkit.Particle.REDSTONE
      case _ =>
        org.bukkit.Particle.valueOf(name)
    }
    loc.getWorld.spawnParticle(particle, loc, amount, offset("x"), offset("y"), offset("z"), dustOptions)
    true
  }
}