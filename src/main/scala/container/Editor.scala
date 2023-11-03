package dev.turtle.grenades
package container

import container.base.{ContainerHolder, ContainerSlot, Item}
import enums.SlotAction
import enums.SlotAction.{Inquire, OpenContainer, PageChange}
import events.ContainerClickEvent
import utils.Conf.{configs, reload}
import utils.extras.{ExtraConfig, ExtraItemStack}
import utils.lang.Message.{clientLang, debugMessage, defaultLang, getLocalizedText}

import com.typesafe.config.*
import dev.turtle.grenades.Main.plugin
import dev.turtle.grenades.utils.conversation.PromptChoice
import org.bukkit.command.CommandSender
import org.bukkit.conversations.{Conversable, Conversation, ConversationFactory}
import org.bukkit.entity.HumanEntity
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.Inventory
import org.bukkit.{Bukkit, Material, event}

import scala.collection.{immutable, mutable}
import scala.jdk.CollectionConverters.*
import scala.util.control.Breaks.*

class Editor(var title: String, var size: Integer, metadataMap: mutable.Map[String, Any]=mutable.Map.empty[String, Any]) extends ContainerHolder {
  override val inventory: Inventory = Bukkit.createInventory(this, size, title)
  override def getInventory: Inventory = inventory
  override def name: String = this.metadata("name").toString
  private var content: mutable.Map[Int, ContainerSlot] = mutable.Map().empty
  private var metadata: mutable.Map[String, Any] = this.metadataMap
  private var page: Int = 0
  private def category: String = metadata("category").toString
  private def config_key: String = metadata("config_key").toString
  private def isRootList: Boolean = category.equalsIgnoreCase(config_key)
  private def containerConfig: Config = configs("container").getConfig(this.name).withFallback(ConfigFactory.empty)

  refreshContents
  def refreshContents: Boolean = {
    inventory.clear()
    if (containerConfig.isPathPresent(s"$category.content"))
      content = containerConfig.findAllContainerSlots(s"$category")
    containerConfig.findString("metadata.fill.by") match {
      case "valuesOfName" =>
        if (isRootList)
          for (key <- configs(category).root().keySet().asScala) {
            getConfigValuesRecursive(key)
          }
        else
          getConfigValuesRecursive(config_key)
      case _ =>
    }
    if ((content.keySet.max >= inventory.getSize) || (this.page > 0)) { //Values are present on other pages, add pagination
      for (slot <- (inventory.getSize - 3).until(inventory.getSize)) {
        var pageActionName: String = "§9previous page"
        var pageOperation: String = "-1"
        var material: Material = Material.STICKY_PISTON
        if (slot == inventory.getSize-2) {
          pageActionName = "§9next page"
          pageOperation = "1"
          material = Material.PISTON
        }
        val slotActions: immutable.Map[SlotAction, immutable.Map[String, String]] = immutable.Map(
          SlotAction.PageChange ->
            immutable.Map( //Slot metadata
              "operation" -> pageOperation
            )
          )
        setItemAtContentIndex(slot, material=material, pageActionName, slotActions)
      }
    }
    if (metadata.contains("nested_config_key") && (last("nested_config_key", "get") ne this.config_key))
      setItemAtContentIndex(inventory.getSize-1, material=Material.REDSTONE_BLOCK, "§9back", immutable.Map(
            SlotAction.OpenContainer ->
              immutable.Map( //Slot metadata
                "category" -> last("nested_category", "get"),
                "config_key" -> last("nested_config_key", "get"),
                "target" -> this.name
              )))
    for ((raw_slot, containerSlot) <- content)
      var slot = raw_slot - page * inventory.getSize
      if (inventory.getSize > slot && (slot >= 0)) {
        inventory.setItem(slot, containerSlot.item.build)
        for ((slotAction, slotMetadata) <- containerSlot.slotActions)
          val slotPath = getSlotPath(slotMetadata)
          val valuePlaceholder = {
            if (!slotPath.isBlank && configs.contains(category))
              configs(category).getValue(slotPath) match {
                case value: ConfigList =>
                  configs(category).findStringList(slotPath)
                case value =>
                  value.unwrapped().toString
              }
            else
              slotAction match {
                case PageChange =>
                  this.page.toString
                case _ =>
                  ""
              }
          }
          val item = inventory.getItem(slot)
          if (valuePlaceholder eq "")
            item.removeLore()
          else
           item.updateLore(Map("value" -> valuePlaceholder))
      }
    true
  }
  private def setItemAtContentIndex(slot: Int, material: Material, displayName: String, slotActions: immutable.Map[SlotAction, immutable.Map[String, String]]): Unit = {
    val containerConfig = configs("container").getConfig(metadata("name").toString)
    val defaultLore: java.util.ArrayList[String] = {
      if (containerConfig.isPathPresent("metadata.default-lore")) {
        java.util.ArrayList(containerConfig
          .getStringList("metadata.default-lore")
          .asScala.map(_.replaceAll("&", "§")).asJava)
      } else
        new java.util.ArrayList[String]
    }
    content += (slot ->
      ContainerSlot(
        slotActions = slotActions,
        item = Item(material)
          .lore(defaultLore)
          .displayName(displayName)
      ))
  }
  private def getConfigValuesRecursive(path: String): Unit = {
    def addItem(slot: Int, displayName: String, cpath: String, openInv: immutable.Map[String, String] = Map.empty): Unit = {
      setItemAtContentIndex(slot, material={
        configs(category).getValue(cpath).valueType() match {
          case ConfigValueType.NUMBER =>
            Material.REDSTONE_LAMP
          case ConfigValueType.BOOLEAN =>
            if (configs(category).getBoolean(cpath))
              Material.GREEN_WOOL
            else
             Material.RED_WOOL
          case ConfigValueType.OBJECT =>
            Material.PUFFERFISH
          case _ =>
            Material.WRITABLE_BOOK
        }
      },displayName,
        immutable.Map(
          if (openInv.isEmpty)
            SlotAction.Inquire ->
              immutable.Map( //Slot metadata
                "config.name" -> s"$category",
                "config.path" -> cpath,
                "set-value" -> {
                  configs(category).getValue(cpath).valueType() match {
                    case ConfigValueType.NUMBER =>
                      "click"
                    case ConfigValueType.BOOLEAN =>
                      "toggle"
                    case _ =>
                      "type-in"
                  }
                }
              )
          else
            SlotAction.OpenContainer -> openInv)
      )
    }
    configs(category).getValue(path).valueType() match {
      case ConfigValueType.OBJECT if !isRootList =>
        for (key <- configs(category).getConfig(path).root().keySet().asScala) {
          val value: ConfigValue = configs(category).getValue(s"$path.$key")
          value.valueType() match {
            case ConfigValueType.OBJECT =>
              breakable {
                for (slot <- 0 to inventory.getSize * 15) { //TODO: Custom max page configuration
                  if (!content.contains(slot)) {
                    addItem(slot,
                      displayName =
                        s"§6$key",
                      openInv = immutable.Map(
                        "category" -> this.category,
                        "config_key" -> s"$path.$key",
                        "target" -> this.name
                      ),
                      cpath = path)
                    break
                  }}}
            case _ =>
              getConfigValuesRecursive(s"$path.$key")
          }
        }
      case default: ConfigValueType =>
        breakable {
          for (slot <- 0 to inventory.getSize*15) { //TODO: Custom max page configuration
            if (!content.contains(slot)) {
              addItem(slot, displayName = s"§6${
                path.replaceAll(
                  s"$config_key."
                  , "")
              }", openInv={
                if (default eq ConfigValueType.OBJECT)
                  immutable.Map(
                    "category" -> this.category,
                    "config_key" -> path,
                    "target" -> this.name
                  )
                else
                 immutable.Map.empty
              },
                cpath=path)
              break
            }
          }}


    }

  }
  override def onContainerClick(e: ContainerClickEvent): Unit = {
    e.setCancelled(true)
    val containerSlot: ContainerSlot = content.getOrElse(e.getSlot+page*inventory.getSize, null)
    if (containerSlot ne null) {
      for ((slotAction, slotMetadata) <- containerSlot.slotActions)
      {
        slotAction match {
          case OpenContainer =>
            openContainer(e.getWhoClicked, slotMetadata)
          case Inquire =>
            inquire(slotMetadata, e.getSlot, e.getClick, e.getWhoClicked)
          case PageChange =>
            val operation = slotMetadata("operation").toString.toInt
            changePage(operation)
          case _ =>
            debugMessage(s"&cSlotAction &2$slotAction &cnot found.", debugLevel=150)
        }
      }
    }
    this.getInventory
  }
  def changePage(operation: Int): Boolean = {
    if ((this.page + operation) >= 0) {
      this.page += operation
      refreshContents
      true
    }
    else
      false
  }
  private def getSlotPath(slotMetadata: Map[String, Any]): String = {
    if (slotMetadata.contains("config.name")) {
      var valuePath: String = slotMetadata("config.path").toString
      if (metadata.contains("config_key"))
        valuePath = valuePath.replaceAll("%config_key%", config_key)
      valuePath
    } else ""
  }
  private def saveValueToConfig(slotMetadata: Map[String, Any], value: Any): Boolean = {
    if (value != null) {
      val valuePath: String = getSlotPath(slotMetadata).toString
      val config: Config = configs(s"${slotMetadata("config.name").toString}")
      config.withValue(valuePath, ConfigValueFactory.fromAnyRef(value)).save()
      reload()
      this.refreshContents
    }
    true
  }
  private def inquire(slotMetadata: Map[String, Any], slot: Integer, click: ClickType, humanEntity: HumanEntity): Unit = {
    if (slotMetadata.contains("config.name")) {
      val valuePath: String = getSlotPath(slotMetadata).toString
      val config: Config = configs(s"${slotMetadata("config.name").toString}")
      val newValue: Any =
        slotMetadata("set-value") match
          case "toggle" =>
            !config.findBoolean(valuePath)
          case "click" =>
            var loadedValue = config.findInt(valuePath)
            var changeAmount = 1
            if (click.isShiftClick)
              changeAmount = 10
            if (click.isRightClick)
              changeAmount *= -1
            (loadedValue+changeAmount)
          case "type-in" =>
            conversation(humanEntity, slotMetadata)
            null
      saveValueToConfig(slotMetadata, newValue)
    }
  }
  private def openContainer(humanEntity: HumanEntity, slotMetadata: Map[String, Any]): Unit = {
    for (key <- Array("nested_category", "nested_config_key")) {
      val keyNonNested = key.replaceAll("nested_", "")
      this.metadata(key) = {
        val slotKeyValue = slotMetadata(keyNonNested).toString
        if (this.metadata.contains(key)) { //Nested path
          if (last(key, "get").equalsIgnoreCase(slotKeyValue) && last(key, "without").nonEmpty) //we are ascending in nested path, remove this path
            last(key, "without")
          else
            s"${this.metadata(key)}|$slotKeyValue" //we are descending, append
        } else {
          s"${this.metadata(keyNonNested)}|$slotKeyValue"
        }
      }
      this.metadata(keyNonNested) = last(key, "get")
    }
    this.metadata("name") = slotMetadata("target").toString.replaceAll("%this%", this.name)
    val newSize = containerConfig.findInt("size", default = Some(this.size))
    val newTitle = containerConfig.findString("title", default = Some(this.metadata("config_key").toString))

    if (newTitle ne this.title)
      changeTitle(newTitle)
    if (newSize ne this.size) {
      humanEntity.closeInventory()
      humanEntity.openInventory(new Editor(this.title,newSize,metadataMap=this.metadata).inventory)
    } else {
      this.page = 0
      this.refreshContents
    }
  }

  private def changeTitle(newTitle: String): Unit = {
    this.title = newTitle
    for (viewer <- this.inventory.getViewers.asScala) {
      viewer.getOpenInventory.setTitle(this.title)
    }
  }

  private def last(path: String, action: String): String = {
    val split = this.metadata(path).toString.split("\\|")
    if (split.length > 1) {
      if (action.equalsIgnoreCase("get"))
        split.last
      else //without last
        split.dropRight(1).mkString("|")
    } else this.metadata(path).toString
  }
  private def conversation(humanEntity: HumanEntity, slotMetadata: Map[String, Any]): Unit = {
    humanEntity.closeInventory() //TODO: Reopen
    val heLang: String = clientLang.getOrElse(humanEntity.getName, defaultLang)
    val factory = new ConversationFactory(plugin)
      .withEscapeSequence("exit")
      .thatExcludesNonPlayersWithMessage("You must be a player to use this command.")
      .withFirstPrompt(PromptChoice.get(slotMetadata("config.path").toString.replaceAll(s"$config_key.", ""), language=heLang))
      .addConversationAbandonedListener((event) => {
        if (event.gracefulExit()) {
          val result = event.getContext.getSessionData("text").asInstanceOf[String]
          humanEntity.sendMessage(getLocalizedText(heLang, "prompt.entered", immutable.Map("entered" -> result)))
          saveValueToConfig(slotMetadata, result)
        } else {
          humanEntity.sendMessage(getLocalizedText(heLang, "prompt.cancelled", Map.empty))
        }
      })
      .withTimeout(30) //TODO: Load timeout from config

    val conversation: Conversation = factory.buildConversation(humanEntity.asInstanceOf[Conversable])
    conversation.begin()
  }
}
