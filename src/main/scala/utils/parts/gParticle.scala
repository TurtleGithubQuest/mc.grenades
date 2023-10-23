package dev.turtle.grenades

package utils.parts

import org.bukkit.{Color, Location}

import scala.collection.mutable

trait T_gParticle(){
  def name: String
  def amount: Integer
  def size: Integer
  def colorHEX: String
  def color: Color
  def offset: mutable.Map[String, Double]
}
class gParticle(
           val name: String,
           val amount: Integer,
           val size: Integer,
           val colorHEX: String,
           val color: Color,
           val colorFade: Color,
           val offset: mutable.Map[String, Double] = mutable.Map("x" -> 0, "y" -> 0, "z" -> 0)
           ) extends T_gParticle {
  def spawn(loc: Location): Unit = {

  }
}
