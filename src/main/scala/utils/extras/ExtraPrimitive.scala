package dev.turtle.grenades
package utils.extras

import org.bukkit.Bukkit.getLogger

import scala.util.{Try, boundary}
import scala.math.{max, min}
import scala.util.boundary.break

object ExtraPrimitive {
implicit class ExtraString(string: String) {
  def isInt: Boolean = Try(Integer.parseInt(string)).isSuccess
  def isDouble: Boolean = Try(string.toDouble).isSuccess
  def isBoolean: Boolean = Try(string.toBoolean).isSuccess
  //def toInt: Integer = Integer.parseInt(string)
}
implicit class ExtraInt(int: Integer) {
  def clamp(minVal: Integer=int, maxVal: Integer=int): Integer = {
    max(min(int, maxVal), minVal)
  }
}
implicit class ExtraArray(array: Array[String]) {

  def getInt(key: String, default: Integer): Integer = {
    var found: Integer = default
    if (array.toList.toString.contains(key))
      boundary {
        for (element <- array) {
          if (element.contains(key)) {
            val elementValue = element.replace(key, "")
            if (elementValue.isInt) {
              found = Integer.parseInt(elementValue)
              break()
            }
          }
        }
      }
    found
  }

  def getBoolean(key: String, default: Boolean): Boolean = {
    var found: Boolean = default
    if (array.toList.toString.contains(key))
      boundary {
        for (element <- array)
          if (element.contains(key)) {
            val elementValue = element.replace(key, "")
            if (elementValue.isBoolean) {
              found = elementValue.toBoolean
              break()
            }
          }
      }
    found
  }
}
}