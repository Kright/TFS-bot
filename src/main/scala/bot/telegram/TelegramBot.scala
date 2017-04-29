package bot.telegram

import bot.telegram.api.Update
import org.json4s._

/**
  * Implementation of bot.telegram bot API.
  * https://core.bot.telegram.org/bots/api
  */

trait TelegramBot {

  implicit val formats = DefaultFormats

  val token: String
  val baseURL: String
  val botURL: String

  def getUpdates(): List[Update]

  def sendMessage(chat_id: Int,
                  text: String,
                  reply_to_message_id: Option[Int] = None,
                  parse_mode: Option[String] = None): Boolean
}


object TelegramBot {

  def apply(token: String): TelegramBot = new APIImpl(token) with Logging
}


