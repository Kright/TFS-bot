package telegram

import org.json4s._
import org.json4s.native.JsonMethods._
import scalaj.http._

/**
  * Implementation of telegram bot API.
  * https://core.telegram.org/bots/api
  *
  * TODO: Need to handle all the exceptions from Http library.
  */
object API {

  implicit val formats = DefaultFormats

  case class User(id: Int, first_name: String, last_name: Option[String], username: Option[String])
  case class Chat(
      id: Int,
      `type`: String,
      username: Option[String],
      first_name: Option[String],
      last_name: Option[String])
  case class MessageEntity(`type`: String, offset: Int, length: Int, url: Option[String])
  case class Message(
      message_id: Int,
      from: Option[User],
      date: Int,
      chat: Chat,
      text: Option[String],
      entities: Option[List[MessageEntity]])
  case class Update(update_id: Int, message: Option[Message])
  case class Response(ok: Boolean, description: Option[String], result: Option[List[Update]])

  private val baseURL = "https://api.telegram.org/"
  private var botURL: String = _

  private var updateRequest: HttpRequest = _
  private var lastUpdateId: Long = 0

  def setToken(token: String): Unit = {
    botURL = baseURL + "bot" + token
    updateRequest = Http(botURL + "/getUpdates")
  }

  def getUpdates(): List[Update] = {
    val json = updateRequest.param("offset", (lastUpdateId + 1).toString).asString

    val response = parse(json.body).extract[Response]

    //TODO: Exception raising in case of error (response.ok == false)

    val updatesList = response.result.get
    if (updatesList.nonEmpty)
      lastUpdateId = updatesList.map(_.update_id).max
    updatesList
  }

  def sendMessage(
      chat_id: Int,
      reply_to_message_id: Option[Int],
      text: String,
      parse_mode: Option[String]): Boolean = {
    var sendRequest = Http(botURL + "/sendMessage").method("POST")
      .param("chat_id", chat_id.toString)
      .param("text", text)

    reply_to_message_id.foreach(id => sendRequest = sendRequest.param("reply_to_message_id", id.toString))
    parse_mode.foreach(mode => sendRequest = sendRequest.param("parse_mode", mode))

    val sendResult = sendRequest.asString
    val response = parse(sendResult.body).extract[Response]

    //TODO: Exception raising in case of error (response.ok == false)

    response.ok
  }
}
