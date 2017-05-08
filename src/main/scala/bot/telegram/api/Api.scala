package bot.telegram.api

import bot.Implicits._
import org.json4s.native.Serialization.write

// https://core.telegram.org/bots/api#update
// not fully implemented
case class Update(update_id: Int, message: Option[Message], callback_query: Option[CallbackQuery]) {
  override def toString: String = {
    val sb = new StringBuilder

    sb.append(s"Update($update_id")
    sb.push(message)
    sb.push(callback_query)
    sb.append(")")

    sb.toString
  }
}

// https://core.telegram.org/bots/api#message
// not fully implemented
case class Message(message_id: Int,
                   from: Option[User],
                   date: Int,
                   chat: Chat,
                   text: Option[String],
                   entities: Option[List[MessageEntity]],
                   sticker: Option[Sticker],
                   contact: Option[Contact]) {

  def isValid: Boolean = {
    if (entities.isEmpty)
      return true

    if (text.isEmpty)
      return false

    val ents = entities.get
    val txt = text.get

    ents.forall(e => e.length + e.offset <= txt.size)
  }

  def getEntities(typeName: String): List[MessageEntity] = {
    for (txt <- text;
         ents <- entities)
      yield ents.filter(_.`type` == typeName)
  }.getOrElse(List.empty)

  def asText(e: MessageEntity): String = {
    assert(text.isDefined)
    text.get.substring(e.offset, e.offset + e.length)
  }

  def botCommands: List[String] = getEntities("bot_command").map(asText)

  override def toString: String = {
    val sb = new StringBuilder

    sb.append(s"Message(id = $message_id")
    sb.append(s", date = $date")
    sb.push(from)
    sb.append(s", chat = $chat")
    sb.push(text)
    sb.push(entities)
    sb.push(sticker)
    sb.append(")")

    sb.toString()
  }
}

// https://core.telegram.org/bots/api#messageentity
case class MessageEntity(`type`: String,
                         offset: Int,
                         length: Int,
                         url: Option[String],
                         user: Option[User])

// https://core.telegram.org/bots/api#user
case class User(id: Int, first_name: String, last_name: Option[String], username: Option[String]) {

  override def toString: String = {
    val sb = new StringBuilder

    sb.append(s"User($first_name)")
    sb.push("last_name", last_name)
    sb.push("username", username)
    sb.append(s", id = $id)")

    sb.toString()
  }
}

// https://core.telegram.org/bots/api#chat
case class Chat(id: Long,
                `type`: String,
                title: Option[String],
                username: Option[String],
                first_name: Option[String],
                last_name: Option[String]) {

  def isPrivate: Boolean = `type` == "private"

  def isGroup: Boolean = `type` == "group"

  def isSupergroup: Boolean = `type` == "supergroup"

  def isChannel: Boolean = `type` == "channel"

  def sendMessage: SendMessage = SendMessage(id.toString, "")

  override def toString: String = {
    val sb = new StringBuilder

    sb.append(s"Chat(id = $id, type = " + `type`)

    sb.push("title", title)
    sb.push("username", username)
    sb.push("first_name", first_name)
    sb.push("last_name", last_name)
    sb.append(")")

    sb.toString()
  }
}

//not fully implemented!!
case class Sticker(file_id: String, width: Integer, height: Integer)


case class Response(ok: Boolean, description: Option[String], result: Option[List[Update]])

case class Contact(phone_number: String, first_name: String, user_id: Int)

case class KeyboardButton(text: String, request_contact: Boolean)

trait KeyboardMarkup {
  implicit val formats = org.json4s.DefaultFormats
  override def toString: String = write(this)
}

case class ReplyKeyboardMarkup(keyboard: List[List[KeyboardButton]], resize_keyboard: Boolean = true, one_time_keyboard: Boolean = true) extends KeyboardMarkup

case class InlineKeyboardMarkup(inline_keyboard: List[List[InlineKeyboardButton]]) extends KeyboardMarkup

case class InlineKeyboardButton(text: String, callback_data: String)

object InlineKeyboardButton {
  def apply(button_data: String): InlineKeyboardButton = new InlineKeyboardButton(button_data, button_data)
}

case class CallbackQuery(id: String,
                         from: User,
                         message: Message,
                         chat_instance: String,
                         data: String)