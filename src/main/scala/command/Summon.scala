package dev.turtle.grenades
package command

import command.base.CMD
import explosions.base.ExplosionType
import utils.Conf.{configs, grenades}
import utils.Grenade
import utils.extras.ExtraConfig
import utils.extras.ExtraPrimitive.*
import utils.lang.Message.debugMessage
import utils.optimized.OnlinePlayers
import utils.parts.GrenadeEntity

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.{Bukkit, Location}

import scala.collection.mutable.ArrayBuffer

object Summon extends CMD{
  val optionalArgs: Array[String] = Array("amount:", "detonate:")
  override def execute(s: CommandSender, args: Array[String]): Boolean = {
    if (args.length == 1) {
      s.sendMessage("commands.summon.list", Map("explosions" -> grenades.keySet.mkString(", ")))
    } else if (args.length >= 2) {
      val permission: String = configs("command").findPermission(args(0), className, perm="summon")
      if (s.hasPerm(permission)) {
        if (grenades.contains(args(1))) {
          val detonationLoc: Location = {
            if (args(2).isDouble) {
              if (args.length > 5)
                new Location(Bukkit.getWorld(args(5)), args(2).toDouble, args(3).toDouble, args(4).toDouble)
              else
                null
            } else {
              val target: Player = Bukkit.getPlayer(args(2))
              if (target ne null)
                target.getLocation
              else
                s.sendMessage("commands.not-found.target", Map("target" -> args(2)))
                null
            }
          }
          if ((detonationLoc ne null) && (detonationLoc.getWorld ne null)) {
            val amountToSpawn: Integer = args.getInt("amount:", 1).clamp(minVal = 1)
            val detonateInstantly: Boolean = args.getBoolean("detonate:", false)
            for (_ <- 1 to amountToSpawn) {
              val grenade: Grenade = grenades(args(1))
              grenade.explosion.spawnAt(detonationLoc)
            }
          } else s.sendMessage("commands.summon.null-location", Map())
        } else s.sendMessage("commands.summon.not-found", Map("explosion" -> args(1)))
      } else {
        s.sendMessage("commands.no-perm", Map("command" -> className))
        debugMessage("console.notify.no-perm", Map("sender"->s.getName, "permission"->permission, "command" -> className), debugLevel=100)
      }
    }
    true
  }

  override def suggestions(s: CommandSender, args: Array[String]): Array[String] = {
    val argsLength: Integer = args.length
    var suggest: ArrayBuffer[String] = ArrayBuffer.empty[String]
    argsLength match
      case 2 =>
        suggest ++= grenades.keys.toArray
      case 3 =>
        s match {
          case player: Player => suggest :+= "%.2f".format(player.getLocation.getX)
        }
        suggest ++= OnlinePlayers.get
      case 4 =>
        if (args(2).isDouble) {
          s match {
            case player: Player => suggest :+= "%.2f".format(player.getLocation.getY)
          }
        } else {
          suggest ++= optionalArgs
        }
      case 5 if s.isInstanceOf[Player] =>
        suggest :+= "%.2f".format(s.asInstanceOf[Player].getLocation.getZ)
      case 6 if s.isInstanceOf[Player] =>
        suggest :+= s.asInstanceOf[Player].getWorld.getName
      case _ =>
        suggest ++= optionalArgs
    suggest.toArray
  }

  override def usage: String = "summon <playerName|coordinates> [amount|detonate]"
}
