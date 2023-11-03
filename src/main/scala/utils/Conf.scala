package dev.turtle.grenades
package utils

import Main.*
import command.base.CMD
import enums.DropLocation
import explosions.base.ExplosionType
import utils.Exceptions.*
import utils.extras.ExtraConfig
import utils.lang.Message.{debugMessage, defaultLang, reloadClientLangs}
import utils.parts.{Explosion, Particle, Sound}

import com.typesafe.config
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import org.bukkit.Bukkit.getLogger
import org.bukkit.{Bukkit, ChatColor}

import java.awt.Color
import java.io.*
import java.nio.file.{Files, StandardCopyOption}
import java.text.DecimalFormat
import java.util
import java.util.List as JavaList
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

object Conf {
  var configs: mutable.Map[String, Config] = mutable.Map()
  /*var cGrenades: Config = ConfigFactory.empty()
  var cExplosions: Config = ConfigFactory.empty()
  var cParticles: Config = ConfigFactory.empty()
  var cSounds: Config = ConfigFactory.empty()
  var cConfig: Config = ConfigFactory.empty()
  var cCommands: Config = ConfigFactory.empty()
  var cLang: Config = ConfigFactory.empty()
  var cContainer: Config = ConfigFactory.empty()
  var cContainerSlots: Config = ConfigFactory.empty()
  var cRecipes: Config = ConfigFactory.empty() //TODO: Reimplement this*/
  var landmines: Config = ConfigFactory.empty()

  var grenades: mutable.Map[String, Grenade] = mutable.Map()
  var explosionTypes: mutable.Map[String, String] = mutable.Map(
    "ANTIMATTER" -> "AntiMatter",
    "CLASSIC" -> "Classic",
    "PROTOTYPE" -> "Prototype",
    "REPLACE" -> "Replace"
  ).withDefault(k => "Classic")

    def getFolderRelativeToPlugin(path: String = ""): String = {
      val filePath = new File(s"plugins/${plugin.getName}/$path")
      val pathSplit = path.split("\\.")
      if (pathSplit.length > 1) {
        val pathString = path.split("/")
        val onlyPath = new File(s"plugins/${plugin.getName}/${
          pathString.dropRight(1).mkString("/")
        }")
        onlyPath.mkdirs()
      } else if (!filePath.exists()) {
          filePath.mkdirs()
        }
      filePath.getPath
    }

    def get(configName: String, toDefaultsFolder: Boolean = false): Config = {
      val pluginFolder = getFolderRelativeToPlugin()
      var configFile = new File(s"${pluginFolder}/$configName.conf")

      if (toDefaultsFolder || !configFile.exists()) { // load default config
        val classLoader = getClass.getClassLoader
        val inputStream: InputStream = classLoader.getResourceAsStream(s"$configName.conf")
        if (toDefaultsFolder) {
          val defaultsPath = getFolderRelativeToPlugin("defaults")
          configFile = new File(s"${defaultsPath}/$configName.conf")
          val splitConfigName = configName.split("/")
          if (splitConfigName.length > 1)
            getFolderRelativeToPlugin(s"defaults/${splitConfigName(0)}")
        }
        if (inputStream != null) {
          try {
            Files.copy(inputStream, configFile.toPath, StandardCopyOption.REPLACE_EXISTING)
            return ConfigFactory.parseFile(configFile)
          } catch {
            case e: Exception =>
              getLogger.info("Error while copying default config values!")
              e.printStackTrace()
          } finally {
            try {
              inputStream.close()
            } catch {
              case e: IOException =>
                getLogger.info("Error while closing InputStream!")
                e.printStackTrace()
            }
          }
        } else {
          debugMessage(s"Resource $configName not found!", debugLevel=200) //TODO: Implement debugger
        }
        ConfigFactory.empty()
      } else {
        ConfigFactory.parseFile(configFile)
      }
    }
    def reloadConfigsInFolder(folderPath: String, fileType: String=".json", toLowerCase: Boolean=false): Boolean = {
      val folder = new File(getFolderRelativeToPlugin(folderPath))
      val files = folder.listFiles()
      if (files != null && files.nonEmpty) {
        for (file <- files) {
          if (file.getName.endsWith(fileType) && file.length() > 1) {
            val fileConfig = ConfigFactory.parseFile(file)
            val fileName = file.getName.split("\\.")(0)
            val updatedFileConfig = fileConfig.withValue({if (toLowerCase) fileName.toLowerCase else fileName}, fileConfig.root())
            folderPath match
              case "data/landmines"  =>
                landmines = landmines.withFallback(updatedFileConfig).resolve()
              case "lang" =>
                configs.update("lang", updatedFileConfig.resolve) //configs("lang") = configs("lang").withFallback(updatedFileConfig).resolve()
              case "container/containers" =>
                configs.update("container", updatedFileConfig.resolve)//configs("container") = configs("container").withFallback(updatedFileConfig).resolve()
              case "container/slots" =>
                configs.update("containerslots", fileConfig.resolve)// = configs("containerslots").withFallback(fileConfig).resolve()
              case "grenades" =>
                configs("grenades") = configs("grenades").withFallback(fileConfig).resolve()
              case _ =>
                debugMessage(s"&cAttempted to load unknown config: $folderPath", Map())
          }
        }
      } else {
        folderPath match //Load default values
          case "lang" =>
            configs.put("lang", ConfigFactory.empty.withValue("en_us", this.get(configName = "lang/en_US").root()))
          case "container/containers" =>
            configs.put("container", ConfigFactory.empty.withValue("editor", this.get(configName = "container/containers/editor").root()))
          case "container/slots" =>
            configs.put("containerslots", ConfigFactory.empty.withValue("slots", this.get(configName = "container/slots/exampleslots").root()))
          case _ =>
            {}
      }
      true
    }
    def reloadGrenades(): Boolean = {
      reloadConfigsInFolder(folderPath="grenades", fileType=".conf", toLowerCase=true)
      configs("grenades") = this.get("grenades").withFallback(this.get("grenades", true))
      for (keyName <- configs("grenades").root().keySet().asScala.toSet) {
        val grenadeCategory: Config = configs("grenades").getConfig(keyName)
        try {
          def getGrenadeInfo(path: String): String =
            grenadeCategory.findString(s"$path")

          def getExplosionInfo(path: String): String =
            configs("explosions").findString(s"${getGrenadeInfo("explosions")}.$path")

          def getParticleInfo(path: String): String =
            configs("particles").findString(s"${getGrenadeInfo("particles")}.$path")

          def getParticleInfoExplosion(path: String): String =
            configs("particles").findString(s"${getExplosionInfo("particles")}.$path")

          def getSoundInfo(path: String): String =
            configs("sounds").findString(s"${getExplosionInfo("sound")}.$path")

          val hexColor = getParticleInfo("color")
          val decodedColor = Color.decode(hexColor)

          grenades.put(keyName,
            new Grenade(
              id = keyName,
              displayName = getGrenadeInfo("item.display-name"),
              lore = getLore(keyName),
              explosion = Explosion(
                explosionType = ExplosionType.fromClass(
                  s"dev.turtle.grenades.explosions.${explosionTypes(getExplosionInfo("name").toUpperCase)}",
                  dropItems = getExplosionInfo("drop-items").toInt,
                  dropLocations = configs("explosions").findStringList(s"${getGrenadeInfo("explosions")}.drop-locations").toArray(Array.empty[String]).map { str =>
                    val enumValue = DropLocation.values.find(_.toString == str)
                    enumValue.getOrElse(throw ConfigValueNotFoundException(s"Invalid DropLocation: $str", 150))
                  },
                  extra= getExplosionInfo("extra")
                ),
                particles = Particle(
                  name = getParticleInfoExplosion("name").toUpperCase,
                  amount = getParticleInfoExplosion("amount").toInt,
                  size = getParticleInfoExplosion("size").toFloat,
                  colorHEX = hexColor,
                  color = org.bukkit.Color.fromRGB(decodedColor.getRed, decodedColor.getGreen, decodedColor.getBlue),
                  colorFade = org.bukkit.Color.fromRGB(decodedColor.getRed, decodedColor.getGreen, decodedColor.getBlue)
                ),
                power = getExplosionInfo("power").toInt,
                damage = getExplosionInfo("damage").toDouble,
                shape=getExplosionInfo("shape").toUpperCase,
                sound = Sound(
                  name = getSoundInfo("name").toLowerCase.replaceAll("_", "."),
                  volume = getSoundInfo("volume").toFloat,
                  pitch = getSoundInfo("pitch").toFloat
                )
              ),
              isLandmine = getGrenadeInfo("is-landmine").toBoolean,
              trail = Particle(
                name = getParticleInfo("name").toUpperCase,
                amount = getParticleInfo("amount").toInt,
                size = getParticleInfo("size").toInt,
                colorHEX = hexColor,
                color = org.bukkit.Color.fromRGB(decodedColor.getRed, decodedColor.getGreen, decodedColor.getBlue),
                colorFade = org.bukkit.Color.fromRGB(decodedColor.getRed, decodedColor.getGreen, decodedColor.getBlue)
              ),
              material = org.bukkit.Material.valueOf(getGrenadeInfo("item.material").toUpperCase),
              glow = getGrenadeInfo("entity.glow").toBoolean,
              model = getGrenadeInfo("entity.model").toUpperCase,
              customNameVisible = getGrenadeInfo("entity.custom-name.visible").toBoolean,
              customName = ChatColor.translateAlternateColorCodes('&',
                getGrenadeInfo("entity.custom-name.value")),
              fuseTime = getGrenadeInfo("entity.fuse").toInt,
              velocity = getGrenadeInfo("entity.velocity").toDouble,
              customModelData = getGrenadeInfo("custom-model-data").toInt
            ))
        } catch {
          case e: ConfigValueNotFoundException =>
            debugMessage(s"&cCouldn't load &4$keyName&c, ${e.message}", debugLevel=e.debugLevel)
        }
      }
      true
    }
    def setValue(config: Config, path: String, value: String): Config = {
      config.withValue(path, ConfigValueFactory.fromAnyRef(value))
    }

    def reload(): Boolean = { //TODO: FIX: Configuration is not being loaded if its being loaded as default
      for (configName <- Array("config", "explosions", "particles", "sounds")) {
        configs(configName) = this.get(configName).withFallback(this.get(configName, true))
      }
      decimalFormat = new DecimalFormat(configs("config").getString("general.decimal-format"))
      debugMode = configs("config").getInt("general.debug")
      pluginPrefix = configs("config").getString("general.plugin.name")
      pluginSep = configs("config").getString("general.plugin.sep")
      reloadConfigsInFolder(folderPath = "lang", fileType = ".conf", toLowerCase=true)
      reloadGrenades()
      reloadConfigsInFolder(folderPath="data/landmines")
      reloadConfigsInFolder(folderPath="container/slots", fileType=".conf", toLowerCase=true)
      reloadConfigsInFolder(folderPath="container/containers", fileType=".conf", toLowerCase=true)
      reloadClientLangs()
      Blocks.reloadVariables(newInterval=configs("config").getLong("general.block-queue.interval"), newMaxBlocksPerLoop=configs("config").getInt("general.block-queue.max-blocks-per-loop"))
      CMD.reload()
      true
    }
    def getLore(grenadeName: String): java.util.ArrayList[String] = {
      var lore: JavaList[String] = configs("grenades").getConfig(grenadeName).getStringList("item.lore")
      if (lore.isEmpty) {
        val placeholderRegex = "%(.*?)%".r
        lore = configs("lang").getStringList(s"$defaultLang.item.grenade.lore").asScala.map { word =>
          val replacedWord = placeholderRegex.replaceAllIn(word.toString, m => {
            val placeholder = m.group(1)
            val splitPlaceholder = placeholder.split("\\|")
            val selectedConfig: Config = {
              if (splitPlaceholder.length <= 1 || !configs.contains(splitPlaceholder(0)))
                configs("grenades").getConfig(grenadeName)
              else configs(splitPlaceholder(0)).getConfig(
                configs("grenades").getString(s"$grenadeName.${splitPlaceholder(0)}")
              )
            }
            if (splitPlaceholder.length <= 1 && selectedConfig.hasPath(placeholder)) {
              selectedConfig.getString(placeholder)
            } else if (splitPlaceholder.length > 1 && selectedConfig.hasPath(splitPlaceholder(1))){
              selectedConfig.getString(splitPlaceholder(1))
            } else m.matched
          })
          replacedWord
        }.asJava
      }
      lore = lore.asScala.map(_.replaceAll("&", "§")).asJava
      java.util.ArrayList(lore)
    }
}
