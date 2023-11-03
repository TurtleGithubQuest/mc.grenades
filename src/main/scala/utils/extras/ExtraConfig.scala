package dev.turtle.grenades
package utils.extras

import container.base.{ContainerSlot, Item}
import enums.SlotAction
import utils.Conf.{configs, getFolderRelativeToPlugin}
import utils.Exceptions.{ConfigContainerSlotNotValidException, ConfigNotFoundException, ConfigPathNotFoundException, ConfigValueNotFoundException}
import utils.lang.Message.debugMessage

import com.typesafe.config.{Config, ConfigFactory, ConfigList, ConfigObject, ConfigRenderOptions, ConfigValue, ConfigValueType}
import org.bukkit.Material

import java.io.{BufferedWriter, File, FileWriter}
import java.util.List as JavaList
import scala.collection.{mutable, immutable}
import scala.util.{Success, Try}
import scala.jdk.CollectionConverters.*

implicit class ExtraConfig(config: Config) {
  def name: String = config.origin.filename

  def isPathPresent(path: String): Boolean = {
    Try(config.hasPath(path)) match {
      case Success(true) => true
      case _ =>
        false
    }
  }

  def findValue(path: String): Option[ConfigValue] = {
    if (config.isPathPresent(path)) {
      Some(config.getValue(path))
    } else {
      None
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

  def findString(path: String, default: Option[Any]=null): String = {
    if (config.isPathPresent(path))
      config.getString(path)
    else if ((default ne null) && default.isDefined)
      default.get.toString
    else
      throw ConfigValueNotFoundException(s"[${this.name}] Value not found: $path", 150)
  }

  def findInt(path: String, default: Option[Integer]=null): Integer = findString(path, default=default/*{if(default ne null) default.toString else null}*/).toInt

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
    val file = new File({
      if (path.isBlank) {
        config.origin.filename
      } else
        getFolderRelativeToPlugin(path)
    })
    try {
      val writer = new BufferedWriter(new FileWriter(file))
      writer.write({
          config.root().render({
            if (path.isBlank)
              ConfigRenderOptions.defaults().setComments(false).setOriginComments(false).setFormatted(true)
            else
              ConfigRenderOptions.concise()
          })
      })
      writer.close()
    } catch {
      case e: Exception =>
        debugMessage(s"&cError saving '${file.getName}' to path: ${e.getMessage}", debugLevel = 200)
        return false
    }
    true
  }

  def unwrappedValue(value: ConfigValue): Any = value match {
    case list: ConfigList => list.asScala.map(cv => unwrappedValue(cv))
    case obj: ConfigObject => obj.toConfig.toMap
    case value => value.unwrapped()
  }
  def toMap: Map[String, Any] = {
    config
      .entrySet()
      .asScala
      .map { entry =>
        entry.getKey -> unwrappedValue(entry.getValue)
      }
      .toMap
  }
  /**
   * path should be something like (container).(category).content.(slotNumber)
   */
  def findContainerSlot(path: String): ContainerSlot = {
    if (config.isPathPresent(path)) {
      val slotConfig: Config = config.getConfig(path)
      val slotActions = slotConfig.getList("slotAction").asScala.foldLeft(immutable.Map.empty[SlotAction, immutable.Map[String, Any]]) {
        (map, slotActionVal) =>
          try {
            val slotActionConfig = {
              if ((slotActionVal ne null) && (slotActionVal.valueType == ConfigValueType.STRING))
                configs("containerslots").getConfig(slotActionVal.unwrapped.toString)
              else
                ConfigFactory.parseString(slotActionVal.unwrapped.toString)
            }
            val slotActionType = slotActionConfig.findString("type")
            val enumValue = SlotAction.values.find(_.toString == slotActionType)
            val slotAction: SlotAction = enumValue.getOrElse(throw ConfigValueNotFoundException(s"Invalid SlotAction: $slotActionType", 150))
            map.updated(slotAction, {
              slotActionConfig.findValue("metadata") match {
                case Some(metadata: ConfigObject) => metadata.toConfig.toMap
                case _ => immutable.Map.empty
              }
            })
          } catch {
            case e: Exception =>
              throw ConfigContainerSlotNotValidException(s"[${this.name}] ContainerSlot not valid: $path // ${e.getMessage}", 155)
          }
      }
      try {
        val item: Item = Item(
          Material.valueOf(slotConfig.findString("material").toUpperCase))
          .amount(slotConfig.findInt("amount"))
          .displayName(slotConfig.findString("displayName"))
          .lore(java.util.ArrayList({
            if (slotConfig.isPathPresent("lore"))
              slotConfig.findStringList("lore")
            else if (config.isPathPresent("metadata.default-lore"))
              config.findStringList("metadata.default-lore")
            else
              new java.util.ArrayList[String]
          }.asScala.map(_.replaceAll("&", "§")).asJava))
        ContainerSlot(slotActions=slotActions, item=item)
      } catch {
        case e: Exception =>
          throw ConfigContainerSlotNotValidException(s"[${this.name}] ContainerSlot not valid: $path // ${e.getMessage}", 155)
      }
    } else throw ConfigPathNotFoundException(s"[${this.name}] Path not found: $path", 150)
  }

  /**
   * path should be something like (container).(category)
   */
  def findAllContainerSlots(path: String): mutable.Map[Int, ContainerSlot] = {
    val slotKeys = config.getConfig(s"$path.content").root().keySet().asScala
    val containerSlots = slotKeys.foldLeft(mutable.Map.empty[Int, ContainerSlot]) {
      (map, slot) =>
        val containerSlot = findContainerSlot(s"$path.content.$slot")
        map.updated(slot.toInt, containerSlot)
    }
    containerSlots
  }
}

