package dev.turtle.grenades
package utils.extras

import container.base.{ContainerSlot, Item}
import enums.SlotAction
import utils.Conf.getFolderRelativeToPlugin
import utils.Exceptions.{ConfigContainerSlotNotValidException, ConfigPathNotFoundException, ConfigValueNotFoundException, ConfigNotFoundException}
import utils.lang.Message.debugMessage

import com.typesafe.config.{Config, ConfigRenderOptions}
import org.bukkit.Material

import java.io.{BufferedWriter, File, FileWriter}
import java.util.List as JavaList
import scala.collection.immutable
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.{Success, Try}

implicit class ExtraConfig(config: Config) {
  def name: String = config.origin.filename

  def isPathPresent(path: String): Boolean = {
    Try(config.hasPath(path)) match {
      case Success(true) => true
      case _ =>
        false
    }
  }

  def getOrElse(path: String, elsePath: String): Config = {
    {
      if (config.isPathPresent(path))
        config.getConfig(path)
      else if (config.isPathPresent(elsePath))
        config.getConfig(elsePath)
      else
        throw ConfigPathNotFoundException(s"[${this.name}] Path not found: $path", 150)
    }
  }

  def findString(path: String, default: String=null): String = {
    if (config.isPathPresent(path))
      config.getString(path)
    else if (default ne null)
      default
    else
      throw ConfigValueNotFoundException(s"[${this.name}] Value not found: $path", 150)
  }

  def findInt(path: String, default: Integer=null): Integer = findString(path, default={if(default ne null) default.toString else null}).toInt

  def findBoolean(path: String): Boolean = findString(path).toBoolean

  def findDouble(path: String): Double = findString(path).toDouble
  def findConfig(path: String): Config = {
    if (config.isPathPresent(path))
      config.getConfig("path")
    else
      throw ConfigNotFoundException(s"[${this.name}] Configuration not found: $path", 150)
  }
  def findStringList(path: String): JavaList[String] = {
    if (config.isPathPresent(path))
      config.getStringList(path)
    else
      throw ConfigValueNotFoundException(s"[${this.name}] List not found: $path", 150)
  }

  /**
   * Determines if command is alias and acts accordingly.
   * Returns main cmd permission if alias has none or is missing.
   */
  def findPermission(cmdOrAlias: String, cmdClass: String, perm: String = "use"): String = {
    if (config.isPathPresent(cmdOrAlias)) {
      config.findString(s"$cmdOrAlias.permission.$perm")
    } else {
      val aliasPath: String = s"$cmdClass.alias.$cmdOrAlias.permission.$perm"
      if (config.isPathPresent(aliasPath))
        config.findString(s"$cmdClass.alias.$cmdOrAlias.permission.$perm")
      else
        config.findString(s"$cmdClass.permission.$perm")
    }
  }

  def save(path: String = ""): Boolean = {
    val file = new File(getFolderRelativeToPlugin(path))
    try {
      val writer = new BufferedWriter(new FileWriter(file))
      writer.write(config.root().render(ConfigRenderOptions.concise()))
      writer.close()
    } catch {
      case e: Exception =>
        debugMessage(s"&cError saving '${file.getName}' to path: ${e.getMessage}", debugLevel = 200)
        return false
    }
    true
  }

  /**
   * path should be something like (container).(category).content.(slotNumber)
   */
  def findContainerSlot(path: String): ContainerSlot = {
    if (config.isPathPresent(path)) {
      val slotConfig: Config = config.getConfig(path)
      try {
        val slotActionType = slotConfig.getString("slotAction.type")
        val enumValue = SlotAction.values.find(_.toString == slotActionType)
        val slotAction: SlotAction = enumValue.getOrElse(throw ConfigValueNotFoundException(s"Invalid SlotAction: $slotActionType", 150))
        val metadata = slotConfig.findString("metadata", default="")
        val inquire = slotConfig.findString("slotAction.inquire", default="")
        val item: Item = Item(
          Material.valueOf(slotConfig.findString("material").toUpperCase))
          .amount(slotConfig.findInt("amount"))
          .displayName(slotConfig.findString("displayName"))
        ContainerSlot(slotAction=slotAction, value=slotConfig.findString("slotAction.value"), metadata=metadata, inquire=inquire, item=item)
      } catch {
        case e: Exception =>
          throw ConfigContainerSlotNotValidException(s"[${this.name}] ContainerSlot not valid: $path // ${e.getMessage}", 155)
      }
    } else throw ConfigPathNotFoundException(s"[${this.name}] Path not found: $path", 150)
  }

  /**
   * path should be something like (container).(category)
   */
  def findAllContainerSlots(path: String): immutable.Map[Int, ContainerSlot] = {
    val slotKeys = config.getConfig(s"$path.content").root().keySet().asScala
    val containerSlots = slotKeys.foldLeft(immutable.Map.empty[Int, ContainerSlot]) {
      (map, slot) =>
        val containerSlot = findContainerSlot(s"$path.content.$slot")
        map.updated(slot.toInt, containerSlot)
    }
    containerSlots
  }
}

