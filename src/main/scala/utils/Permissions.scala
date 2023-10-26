package dev.turtle.grenades
package utils

import utils.Conf.cConfig
import utils.lang.Message.debugMessage

import com.typesafe.config.Config
import org.bukkit.Bukkit.getConsoleSender
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
    if (!hasPerm)
      debugMessage("console.notify.no-perm", Map("sender" -> s.getName))
    hasPerm
  }

  def get(a: String, cmdClass:String, perm: String="use"): String = {
      if (cCommands.getIsNull(a)) {
        cCommands.getString(s"$cmdClass.alias.$a.permission.$perm")
      } else {
        cCommands.getString(s"$a.permission.$perm")
      }

  }
}
