package telegram

import org.json4s._

/**
  * Implementation of telegram bot API.
  * https://core.telegram.org/bots/api
  */

trait TelegramAPI {

  implicit val formats = DefaultFormats

  val token: String
  val baseURL: String
  val botURL: String

  def getUpdates(): List[Update]

  def sendMessage(chat_id: Int, reply_to_message_id: Option[Int], text: String, parse_mode: Option[String]): Boolean
}


object TelegramAPI {

  def apply(token: String): TelegramAPI = new APIImpl(token)
}


case class User(id: Int, first_name: String, last_name: Option[String], username: Option[String])

case class MessageEntity(`type`: String, offset: Int, length: Int, url: Option[String])

case class Update(update_id: Int, message: Option[Message])

case class Response(ok: Boolean, description: Option[String], result: Option[List[Update]])

case class Message(message_id: Int,
                   from: Option[User],
                   date: Int,
                   chat: Chat,
                   text: Option[String],
                   entities: Option[List[MessageEntity]])

case class Chat(id: Int, `type`: String,
                username: Option[String],
                first_name: Option[String],
                last_name: Option[String])
