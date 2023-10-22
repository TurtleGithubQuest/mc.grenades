package dev.turtle.grenades

import utils.Conf.*
import Main.{coreprotectapi, debugMode, decimalFormat, plugin, random}

import de.tr7zw.changeme.nbtapi.NBTItem

import dev.turtle.grenades.command.base.CMD
import dev.turtle.grenades.events.bukkit.InteractEvent
import dev.turtle.grenades.utils.Conf
import net.coreprotect.{CoreProtect, CoreProtectAPI}
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin

import java.text.DecimalFormat
import scala.collection.mutable
import scala.collection.mutable.Map
import scala.jdk.CollectionConverters.*
import scala.util.Random

object Main {
  var debugMode = false
  var pluginPrefix = "Grenade"
  var pluginSep = ":"
  var plugin: JavaPlugin = _
  var owedItems: mutable.Map[Player, NBTItem] = mutable.Map() //Map[Player, NBTItem] = null
  var cooldown: mutable.Map[String, Long] = mutable.Map().withDefault(k => (System.currentTimeMillis))
  var random: Random = _
  var coreprotectapi: CoreProtectAPI = null
  var decimalFormat = new DecimalFormat("##0.#")
}

class Main extends JavaPlugin {

  override def onEnable(): Unit = {
    plugin = this
    random = new Random
    Conf.reload()
    getCommand("grenade").setExecutor(CMD)
    this.getServer.getPluginManager.registerEvents(new InteractEvent, this)
    var recipecount = 0
    var i = 0
   /* if (Core.cfg.getBoolean("landmine.enabled")) {
      if (Core.cfg.getBoolean("landmine.hiding")) landmineHiderCaretaker
      this.getServer.getPluginManager.registerEvents(new Nothing, this)
    }*/
    hookCoreProtect
  }

  override def onDisable(): Unit = {
    var recipecount = 0
    var i = 0
  }

  private def hookCoreProtect: Unit = {
    val plugin = getServer.getPluginManager.getPlugin("CoreProtect")
    // Check that CoreProtect is loaded
    if (plugin == null || !plugin.isInstanceOf[CoreProtectAPI]) {return}
    // Check that the API is enabled
    val CoreProtect: CoreProtectAPI = plugin.asInstanceOf[CoreProtectAPI]
    if (!CoreProtect.isEnabled) {return}
    // Check that a compatible version of the API is loaded
    if (CoreProtect.APIVersion < 9 || !Conf.cConfig.getBoolean("hooks.coreprotect")) {return}
    //log("Hooked into CoreProtect")
    coreprotectapi = CoreProtect
  }
}