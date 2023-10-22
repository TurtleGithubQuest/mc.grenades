package dev.turtle.grenades

package utils

import Main.*
import com.typesafe.config.{Config, ConfigFactory}
import Conf.*
import dev.turtle.grenades.utils
import utils.parts.{Explosion, ExplosionType, gParticle, gSound}
import dev.turtle.grenades.explosions.{AntiMatter, Classic, Replace}
import dev.turtle.grenades.utils.lang.Message.clientLang
import org.bukkit.Bukkit
import org.bukkit.Bukkit.getLogger

import java.awt.Color
import java.io.{File, IOException, InputStream}
import java.nio.file.{Files, StandardCopyOption}
import java.text.DecimalFormat
import java.util
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import java.util.List as JavaList

object Conf {
  var cGrenades: Config = ConfigFactory.empty()
  var cExplosions: Config = ConfigFactory.empty()
  var cParticles: Config = ConfigFactory.empty()
  var cSounds: Config = ConfigFactory.empty()
  var cConfig: Config = ConfigFactory.empty()
  var cLang: Config = ConfigFactory.empty()
  var cRecipes: Config = ConfigFactory.empty() //TODO: Reimplement this

  var grenades: mutable.Map[String, Grenade] = mutable.Map()
  var explosionTypes: mutable.Map[String, ExplosionType] = mutable.Map(
    "ANTIMATTER" -> AntiMatter,
    "REPLACE" -> Replace,
    "CLASSIC" -> Classic
  ).withDefault(k => Classic)

    def getFolder(path: String = ""): String = {
      val filePath = new File(s"plugins/${plugin.getName}/$path")
      if (!filePath.exists()) {
        filePath.mkdirs()
      }
      filePath.getPath
    }

    def get(configName: String, defaultValues: Boolean = false): Config = {
      val pluginFolder = getFolder()
      var configFile = new File(s"${pluginFolder}/$configName.conf")

      if (!configFile.exists() || defaultValues) { // load default config
        val classLoader = getClass.getClassLoader
        val inputStream: InputStream = classLoader.getResourceAsStream(s"$configName.conf")
        if (defaultValues) {
          val defaultsPath = getFolder("defaults")
          configFile = new File(s"${defaultsPath}/$configName.conf")
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

    def reloadGrenades(): Boolean = {
      val grenadeTypesFolder = new File(getFolder("grenades"))
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
        def getGrenadeInfo(path: String): String = {
          grenadeCategory.getString(s"$path")
        }
        def getExplosionInfo(path: String): String = {
          cExplosions.getString(s"${getGrenadeInfo("explosion")}.$path")
        }
        def getParticleInfo(path: String): String = {
          cParticles.getString(s"${getGrenadeInfo("particles")}.$path")
        }
        def getParticleInfoExplosion(path: String): String = {
          cParticles.getString(s"${getExplosionInfo("particles")}.$path")
        }
        def getSoundInfo(path: String): String = {
          cSounds.getString(s"${getExplosionInfo("sound")}.$path")
        }
        val hexColor = getParticleInfo("color")
        val decodedColor = Color.decode(hexColor)

        grenades.put(keyName,
          new Grenade(
            id = keyName,
            displayName = getGrenadeInfo("display-name"),
            lore = getLore(keyName),
            explosion = new Explosion(
              name = explosionTypes(getExplosionInfo("name").toUpperCase),
              particles = new gParticle(
                name = getParticleInfoExplosion("name").toUpperCase,
                amount = getParticleInfoExplosion("amount").toInt,
                size = getParticleInfoExplosion("size").toInt,
                colorHEX = hexColor,
                color = org.bukkit.Color.fromRGB(decodedColor.getRed, decodedColor.getGreen, decodedColor.getBlue),
                colorFade = org.bukkit.Color.fromRGB(decodedColor.getRed, decodedColor.getGreen, decodedColor.getBlue)
              ),
              power = getExplosionInfo("power").toInt,
              shape = getExplosionInfo("shape").toUpperCase,
              sound = new gSound(
                name = getSoundInfo("name").toLowerCase.replaceAll("_", "."),
                volume = getSoundInfo("volume").toFloat,
                pitch = getSoundInfo("pitch").toFloat
              ),
              extra = getExplosionInfo("extra")
            ),
            isLandmine = getGrenadeInfo("is-landmine").toBoolean,
            trail = new gParticle(
              name = getParticleInfo("name").toUpperCase,
              amount = getParticleInfo("amount").toInt,
              size = getParticleInfo("size").toInt,
              colorHEX = hexColor,
              color = org.bukkit.Color.fromRGB(decodedColor.getRed, decodedColor.getGreen, decodedColor.getBlue),
              colorFade = org.bukkit.Color.fromRGB(decodedColor.getRed, decodedColor.getGreen, decodedColor.getBlue)
            ),
            material = org.bukkit.Material.valueOf(getGrenadeInfo("material").toUpperCase),
            glow = getGrenadeInfo("glow").toBoolean,
            model = getGrenadeInfo("model").toUpperCase,
            countdownVisible = getGrenadeInfo("countdown.visible").toBoolean,
            countdownTime = getGrenadeInfo("countdown.time").toInt,
            velocity = getGrenadeInfo("velocity").toDouble
        ))
      }
      true
    }

    def reload(): Boolean = {
      cConfig = this.get("config").withFallback(this.get("config", true))
      cLang = this.get("lang").withFallback(this.get("lang", true))
      cExplosions = this.get("explosions").withFallback(this.get("explosions", true))
      cParticles = this.get("particles").withFallback(this.get("particles", true))
      cSounds = this.get("sounds").withFallback(this.get("sounds", true))
      decimalFormat = new DecimalFormat(cConfig.getString("general.decimal-format"))
      debugMode = cConfig.getBoolean("general.debug")
      pluginPrefix = cConfig.getString("general.plugin.name")
      pluginSep = cConfig.getString("general.plugin.sep")
      reloadGrenades()
      true
    }
    def getLore(grenadeName: String): java.util.ArrayList[String] = {
      val cfg: Map[String, Config] = Map(
        "grenade" -> cGrenades.getConfig(s"$grenadeName"),
        "explosion" -> cExplosions,
        "particles" -> cParticles
      )

      var lore: JavaList[String] = cfg("grenade").getStringList("lore")
      if (lore.isEmpty) {
        val placeholderRegex = "%(.*?)%".r
        lore = cLang.getStringList("item.grenade.lore").asScala.map { word =>
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
      //new java.util.ArrayList[String](loreList.asJava)
      java.util.ArrayList(lore)
    }
}
