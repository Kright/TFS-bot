package bot.telegram.api

import bot.Implicits._

// https://core.telegram.org/bots/api#update
// not fully implemented
case class Update(update_id: Int, message: Option[Message]) {
  override def toString: String = {
    val sb = new StringBuilder

    sb.append(s"Update($update_id")
    sb.push(message)
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
                   sticker: Option[Sticker]) {

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
         ents <- entities) {
      return ents.filter(_.`type` == typeName)
    }
    List.empty
  }

  def asText(e: MessageEntity): String = {
    assert(text.isDefined)
    text.get.substring(e.offset, e.offset + e.length)
  }

  def botCommands = getEntities("bot_command").map(asText)

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

  def isPrivate = `type` == "private"

  def isGroup = `type` == "group"

  def isSupergroup = `type` == "supergroup"

  def isChannel = `type` == "channel"

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


sealed trait SendResult

case object SendSuccess extends SendResult

case class SendFailed(reason: Response) extends SendResult

object SendResult {
  def apply(response: Response): SendResult =
    if (response.ok)
      SendSuccess
    else
      SendFailed(response)
}
