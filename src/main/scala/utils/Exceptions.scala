package dev.turtle.grenades
package utils

object Exceptions {
  case class ConfigValueNotFoundException(message: String, debugLevel: Integer) extends Exception(message)

  case class ConfigPathNotFoundException(message: String, debugLevel: Integer) extends Exception(message)
  case class ConfigContainerSlotNotValidException(message: String, debugLevel: Integer) extends Exception(message)
  case class ConfigNotFoundException(message: String, debugLevel: Integer) extends Exception(message)
}
