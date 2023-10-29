package dev.turtle.grenades

import Main.*
import command.base.CMD
import container.Editor
import container.base.ContainerHolder
import listeners.base.ExtraListener.registerAllEvents
import utils.Conf
import utils.Conf.*
import utils.lang.Message.debugMessage

import de.tr7zw.changeme.nbtapi.NBTItem
import net.coreprotect.CoreProtectAPI
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

import java.text.DecimalFormat
import scala.collection.mutable.Map
import scala.collection.{immutable, mutable}
import scala.util.Random

object Main {
  var debugMode: Integer = 1
  var pluginPrefix = "Grenade"
  var pluginSep = ":"
  var plugin: JavaPlugin = _
  var owedItems: mutable.Map[Player, NBTItem] = mutable.Map()
  var cooldown: mutable.Map[String, Long] = mutable.Map().withDefault(k => (System.currentTimeMillis))
  var random: Random = _
  var coreprotectapi: CoreProtectAPI = null
  var decimalFormat = new DecimalFormat("##0.#")
}

class Main extends JavaPlugin {

  override def onEnable(): Unit = {
    plugin = this
    random = new Random
    /*var te: Array[ContainerHolder] = Array(new Editor("ed1", 5), new Editor("ed2", 5))
    for (t <- te)
      getLogger.info(t.className)*/
    Conf.reload()
    getCommand("grenade").setExecutor(CMD)
    registerAllEvents
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
              {}
          debugMessage(s"$pluginPrefix$pluginSep &eshook hands with $pluginName.", immutable.Map())
      }
    }
  }
}