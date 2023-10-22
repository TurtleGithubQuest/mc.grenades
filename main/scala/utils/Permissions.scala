package dev.turtle.grenades
package utils

import com.typesafe.config.Config
import utils.Conf.cConfig
import org.bukkit.command.CommandSender

object Permissions {
  val cCommands: Config = cConfig.getConfig("commands")

  def has(s: CommandSender, permission: String): Boolean = {
    val permParts = permission.split("\\.")
    val permGroup = s"${permParts(0)}.${permParts(1)}"
    var hasPerm = s.hasPermission(permission)
    if (permParts.length > 2)
      if (s.hasPermission(s"$permGroup.*") ||
          s.hasPermission(s"$permGroup")
          )
        hasPerm = true
    hasPerm
  }
}
