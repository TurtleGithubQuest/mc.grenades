package dev.turtle.grenades
package utils.lang

import Main.{debugMode, pluginPrefix, pluginSep}
import utils.Conf.{configs, getFolderRelativeToPlugin}
import utils.extras.{ExtraCommandSender, ExtraConfig}

import com.typesafe.config.{ConfigException, ConfigFactory, ConfigValueFactory}
import net.md_5.bungee.api.ChatMessageType
import org.bukkit.Bukkit.getConsoleSender
import org.bukkit.ChatColor

import java.io.{File, FileNotFoundException}
import scala.collection.{immutable, mutable}
import scala.jdk.CollectionConverters.*

object Message extends ExtraCommandSender {
  val cMessaging = configs("config").getConfig("general.messaging")
  val defaultLang = cMessaging.getString("default.lang").toLowerCase
  val placeholderPrefix = cMessaging.getString("default.prefix.placeholder")
  val textPrefix = cMessaging.getString("default.prefix.text")
  var clientLang: mutable.Map[String, String] = mutable.Map().withDefault(k => defaultLang)
  /**
   * 1 - 49 = Trace
   * 50 - 99 = Debug
   * 100 - 149 = Info
   * 150 - 199 = Warn
   * 200 - 249 = Error
   * 250+      = Fatal
   */
  def debugMessage(path: String, placeholders: immutable.Map[String, String]=immutable.Map(), chatMessageType: ChatMessageType = ChatMessageType.CHAT, debugLevel: Integer = 1): Boolean = {
    if (debugLevel >= debugMode || debugLevel > 249)
      getConsoleSender.sendMessage(s"$pluginPrefix$pluginSep$path", placeholders, chatMessageType)
    true
  }
  def reloadClientLangs(): Boolean = {
    saveClientLangs()
    loadClientLangs()
  }
  def saveClientLangs(): Boolean = {
    if (clientLang.isEmpty)
      loadClientLangs()
    ConfigFactory.empty().withValue("clientLang",ConfigValueFactory.fromMap(clientLang.asJava))
      .save("data/clientLang.json")
  }
  def loadClientLangs(): Boolean = {
    clientLang = try {
      val config = ConfigFactory.parseFile(new File(getFolderRelativeToPlugin("data/clientLang.json")))
      config.getObject("clientLang")
        .unwrapped()
        .asScala
        .toMap
        .view
        .mapValues(_.toString)
        .to(collection.mutable.Map)
        .withDefault(k => defaultLang)
    } catch {
      case _: FileNotFoundException | _: ConfigException => mutable.Map().withDefault(k => defaultLang)
    }
    true
  }

  def getLocalizedText(language: String, path: String, placeholders: immutable.Map[String, String]=Map.empty): String = {
    var text: String = {
      try {
        textPrefix + configs("lang").findString(s"$language.$path")
      } catch {
        case e: Throwable =>
          path
      }
    }
    if (placeholders.nonEmpty)
      for ((key, value) <- placeholders) {
        text = text.replace(s"%$key%", {
          if (value.contains("&")) value
          else placeholderPrefix + value
        } + textPrefix)
      }
    ChatColor.translateAlternateColorCodes('&', text)
  }
}
