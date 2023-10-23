package dev.turtle.grenades

package utils.parts

import org.bukkit.Sound

trait T_Explosion {
  def name: ExplosionType
  def particles: gParticle
  def shape: String
  def power: Integer
  def sound: gSound
  def extra: String
}
class Explosion(
                val name: ExplosionType,
                val particles: gParticle,
                val power: Integer,
                val shape: String,
                val sound: gSound,
                val extra: String
                   ) extends T_Explosion {

}
