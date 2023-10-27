package dev.turtle.grenades

package utils.parts

import explosions.base.GrenadeExplosion

trait T_Explosion {
  def name: GrenadeExplosion
  def particles: gParticle
  def shape: String
  def power: Integer
  def damage: Double
  def dropItems: Integer
  def sound: gSound
  def extra: String
}
class Explosion(
                val name: GrenadeExplosion,
                val particles: gParticle,
                val power: Integer,
                val damage: Double,
                val dropItems: Integer,
                val shape: String,
                val sound: gSound,
                val extra: String
                   ) extends T_Explosion {

}
