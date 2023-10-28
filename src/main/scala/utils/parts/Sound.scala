package dev.turtle.grenades
package utils.parts

import org.bukkit.Location

case class Sound(
            name: String,
            volume: Float,
            pitch: Float
            ){
  def playAt(loc: Location): Boolean = {
    loc.getWorld.playSound(loc, name, volume, pitch)
    true
  }
}