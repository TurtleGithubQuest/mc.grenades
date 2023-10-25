package dev.turtle.grenades

import Main.*
import command.base.CMD
import events.bukkit.{InteractEvent, LandmineEvents}
import utils.Conf
import utils.Conf.*
import utils.lang.Message.debugMessage

import com.sk89q.worldedit.bukkit.WorldEditPlugin
import de.tr7zw.changeme.nbtapi.NBTItem
import dev.turtle.grenades.utils.optimized.OnlinePlayers
import net.coreprotect.CoreProtectAPI
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

import java.text.DecimalFormat
import scala.collection.mutable.Map
import scala.collection.{immutable, mutable}
import scala.util.Random

object Main {
  var debugMode = false
  var pluginPrefix = "Grenade"
  var pluginSep = ":"
  var plugin: JavaPlugin = _
  var owedItems: mutable.Map[Player, NBTItem] = mutable.Map()
  var cooldown: mutable.Map[String, Long] = mutable.Map().withDefault(k => (System.currentTimeMillis))
  var random: Random = _
  var coreprotectapi: CoreProtectAPI = null
  var faweapi: WorldEditPlugin = null
  var decimalFormat = new DecimalFormat("##0.#")
}

class Main extends JavaPlugin {

  override def onEnable(): Unit = {
    plugin = this
    random = new Random
    Conf.reload()
    getCommand("grenade").setExecutor(CMD)
    this.getServer.getPluginManager.registerEvents(new InteractEvent, this)
    if (cConfig.getBoolean("landmine.enabled"))
      this.getServer.getPluginManager.registerEvents(new LandmineEvents, this)
    this.getServer.getPluginManager.registerEvents(OnlinePlayers, this)
    hookPlugins
  }

  override def onDisable(): Unit = {
    var recipecount = 0
    var i = 0
  }
  private def hookPlugins: Unit = {
    for (pluginName <- Array("CoreProtect", "FastAsyncWorldEdit")) {
      val plugin = getServer.getPluginManager.getPlugin(pluginName)
      if (plugin != null && plugin.isEnabled) {
        if (Conf.cConfig.getBoolean(s"hooks.${pluginName.toLowerCase}"))
          pluginName match
            case "CoreProtect" =>
              coreprotectapi = plugin.asInstanceOf[CoreProtectAPI]
            case "FastAsyncWorldEdit" =>
              faweapi = plugin.asInstanceOf[WorldEditPlugin]
          debugMessage(Bukkit.getConsoleSender, s"$pluginPrefix$pluginSep &eshook hands with $pluginName.", immutable.Map())
      }
    }
  }
}