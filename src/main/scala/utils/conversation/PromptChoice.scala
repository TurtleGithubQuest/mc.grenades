package dev.turtle.grenades
package utils.conversation

import com.typesafe.config.Config
import org.bukkit.conversations.{Conversation, ConversationContext, FixedSetPrompt, Prompt, StringPrompt}
import utils.extras.ExtraConfig

import dev.turtle.grenades.utils.Conf.configs
import dev.turtle.grenades.utils.lang.Message.getLocalizedText

import scala.collection.immutable
import scala.jdk.CollectionConverters.*
class TextPrompt(path: String, language: String) extends StringPrompt {
  override def getPromptText(context: ConversationContext): String = {
    getLocalizedText(language, "prompt.string.text", immutable.Map("name" -> path))
  }
  override def acceptInput(context: ConversationContext, input: String): Prompt = {
    if (!input.equalsIgnoreCase("exit"))
      context.setSessionData("text", input)
    Prompt.END_OF_CONVERSATION
  }
}
class ConfigValuePrompt(config: Config, language: String) extends FixedSetPrompt(config.root.keySet.asScala.toArray: _*) {
  override def getPromptText(context: ConversationContext): String = {
    getLocalizedText(language, "prompt.configvalue.text", immutable.Map("available" -> formatFixedSet))
  }

  override def acceptValidatedInput(context: ConversationContext, input: String): Prompt = {
    if (input.equalsIgnoreCase("exit"))
      Prompt.END_OF_CONVERSATION
    else if (formatFixedSet.contains(input)){
      context.setSessionData("text", input)
      Prompt.END_OF_CONVERSATION
    } else this
  }
}
object PromptChoice:
  def get(configName: String, language: String): Prompt = {
    if (configs.contains(configName))
      ConfigValuePrompt(configs(configName), language)
    else
      TextPrompt(configName, language)
  }