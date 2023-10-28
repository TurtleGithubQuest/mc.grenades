package dev.turtle.grenades
package utils.extras

import com.typesafe.config.{Config, ConfigRenderOptions}
import dev.turtle.grenades.utils.Conf.getFolderRelativeToPlugin
import dev.turtle.grenades.utils.Exceptions.{ConfigPathNotFoundException, ConfigValueNotFoundException}
import dev.turtle.grenades.utils.lang.Message.debugMessage

import java.io.{BufferedWriter, File, FileWriter}
import scala.util.{Success, Try}
import java.util.List as JavaList

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

  def findString(path: String): String = {
    if (config.isPathPresent(path))
      config.getString(path)
    else
      throw ConfigValueNotFoundException(s"[${this.name}] Value not found: $path", 150)
  }

  def findInt(path: String): Integer = findString(path).toInt

  def findBoolean(path: String): Boolean = findString(path).toBoolean

  def findDouble(path: String): Double = findString(path).toDouble

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
}

