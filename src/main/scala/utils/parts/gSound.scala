package dev.turtle.grenades
package utils.parts

trait T_gSound(){
  def name: String
  def volume: Float
  def pitch: Float
}
class gSound(
            val name: String,
            val volume: Float,
            val pitch: Float
            ) extends T_gSound {}
