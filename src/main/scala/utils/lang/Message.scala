package dev.turtle.grenades
package utils.lang

import Main.debugMode
import utils.Conf.{cConfig, cLang, getFolderRelativeToPlugin, save}

import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import dev.turtle.grenades.utils.extras.ExtraCommandSender
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit.getConsoleSender
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

import java.io.{File, FileNotFoundException}
import scala.collection.{immutable, mutable}
import scala.jdk.CollectionConverters.*

object Message extends ExtraCommandSender {
  val cMessaging = cConfig.getConfig("general.messaging")
  val defaultLang = cMessaging.getString("default.lang").toLowerCase
  val placeholderPrefix = cMessaging.getString("default.prefix.placeholder")
  val textPrefix = cMessaging.getString("default.prefix.text")
  var clientLang: mutable.Map[String, String] = mutable.Map().withDefault(k => defaultLang)

  def debugMessage(path: String, placeholders: immutable.Map[String, String], chatMessageType: ChatMessageType = ChatMessageType.CHAT): Boolean = {
    if (debugMode)
      getConsoleSender.sendMessage(path, placeholders, chatMessageType)
    true
  }
  def reloadClientLangs(): Boolean = {
    saveClientLangs()
    loadClientLangs()
  }
  def saveClientLangs(): Boolean = {
    save(
      ConfigFactory.empty().withValue("clientLang", ConfigValueFactory.fromMap(clientLang.asJava)),
      "data/clientLang.json"
    )
  }
  def loadClientLangs(): Boolean = {
    clientLang = try {
      val config = ConfigFactory.parseFile(
        new File(getFolderRelativeToPlugin("data/clientLang.json"))
      )
      config.getObject("clientLang")
        .unwrapped()
        .asScala
        .toMap
        .view
        .mapValues(_.toString).to(collection.mutable.Map)
    } catch {
      case _: FileNotFoundException => mutable.Map().withDefault(k => defaultLang)
    }
    true
  }
}
