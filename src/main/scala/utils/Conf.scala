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
  var cGrenades: Config = ConfigFactory.empty()
  var cExplosions: Config = ConfigFactory.empty()
  var cParticles: Config = ConfigFactory.empty()
  var cSounds: Config = ConfigFactory.empty()
  var cConfig: Config = ConfigFactory.empty()
  var cCommands: Config = ConfigFactory.empty()
  var cLang: Config = ConfigFactory.empty()
  var cRecipes: Config = ConfigFactory.empty() //TODO: Reimplement this
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
          getLogger.info(s"Resource $configName not found!") //TODO: Implement debugger
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
                cLang = cLang.withFallback(updatedFileConfig).resolve()
              case _ =>
                debugMessage(s"&cAttempted to load unknown config: $folderPath", Map())
          }
        }
      } else {
        folderPath match
          case "lang" =>
            cLang = cLang.withValue("en_us", this.get(configName = "lang/en_US").root())
          case _ =>
            {}
      }
      true
    }
    def reloadGrenades(): Boolean = {
      val grenadeTypesFolder = new File(getFolderRelativeToPlugin("grenades"))
      val grenadeTypesFiles = grenadeTypesFolder.listFiles()
      if (grenadeTypesFiles != null){
        for (grenadeTypeFile <- grenadeTypesFiles) {
          if (grenadeTypeFile.getName.endsWith(".conf")) {
            val fileConfig = ConfigFactory.parseFile(grenadeTypeFile)
            cGrenades = cGrenades.withFallback(fileConfig).resolve()
          }
        }
      }
      cGrenades = this.get("grenades").withFallback(this.get("grenades", true))
      for (keyName <- cGrenades.root().keySet().asScala.toSet) {
        val grenadeCategory: Config = cGrenades.getConfig(keyName)
        try {
          def getGrenadeInfo(path: String): String = {
            grenadeCategory.findString(s"$path")
          }

          def getExplosionInfo(path: String): String = {
            cExplosions.findString(s"${getGrenadeInfo("explosion")}.$path")
          }

          def getParticleInfo(path: String): String = {
            cParticles.findString(s"${getGrenadeInfo("particles")}.$path")
          }

          def getParticleInfoExplosion(path: String): String = {
            cParticles.findString(s"${getExplosionInfo("particles")}.$path")
          }

          def getSoundInfo(path: String): String = {
            cSounds.findString(s"${getExplosionInfo("sound")}.$path")
          }

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
                  dropLocations = cExplosions.findStringList(s"${getGrenadeInfo("explosion")}.drop-locations").toArray(Array.empty[String]).map { str =>
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

    def reload(): Boolean = {
      cConfig = this.get("config").withFallback(this.get("config", true))
      reloadConfigsInFolder(folderPath = "lang", fileType = ".conf", toLowerCase=true)
      cExplosions = this.get("explosions").withFallback(this.get("explosions", true))
      cParticles = this.get("particles").withFallback(this.get("particles", true))
      cSounds = this.get("sounds").withFallback(this.get("sounds", true))
      decimalFormat = new DecimalFormat(cConfig.getString("general.decimal-format"))
      debugMode = cConfig.getInt("general.debug")
      pluginPrefix = cConfig.getString("general.plugin.name")
      pluginSep = cConfig.getString("general.plugin.sep")
      reloadGrenades()
      reloadConfigsInFolder(folderPath="data/landmines")
      reloadClientLangs()
      Blocks.reloadVariables(newInterval=cConfig.getLong("general.block-queue.interval"), newMaxBlocksPerLoop=cConfig.getInt("general.block-queue.max-blocks-per-loop"))
      CMD.reload()
      true
    }
    def getLore(grenadeName: String): java.util.ArrayList[String] = {
      val cfg: Map[String, Config] = Map(
        "grenade" -> cGrenades.getConfig(s"$grenadeName"),
        "explosion" -> cExplosions,
        "particles" -> cParticles
      )

      var lore: JavaList[String] = cfg("grenade").getStringList("item.lore")
      if (lore.isEmpty) {
        val placeholderRegex = "%(.*?)%".r
        lore = cLang.getStringList(s"$defaultLang.item.grenade.lore").asScala.map { word =>
          val replacedWord = placeholderRegex.replaceAllIn(word.toString, m => {
            val placeholder = m.group(1)
            val splitPlaceholder = placeholder.split("\\|")
            val config: Config = {
              if (splitPlaceholder.length <= 1 || !cfg.contains(splitPlaceholder(0))) cfg("grenade")
              else cfg(splitPlaceholder(0)).getConfig(
                cGrenades.getString(s"$grenadeName.${splitPlaceholder(0)}")
              )
            }
            if (splitPlaceholder.length <= 1 && config.hasPath(placeholder)) {
              config.getString(placeholder)
            } else if (splitPlaceholder.length > 1 && config.hasPath(splitPlaceholder(1))){
              config.getString(splitPlaceholder(1))
            } else m.matched
          })
          replacedWord
        }.asJava
      }
      lore = lore.asScala.map(_.replaceAll("&", "§")).asJava
      java.util.ArrayList(lore)
    }
}
