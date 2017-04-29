package telegram

import org.json4s._
import org.json4s.native.JsonMethods._
import scalaj.http._

/**
  * Created by lgor on 4/15/17.
  *
  * TODO: Need to handle all the exceptions from Http library.
  */
class APIImpl(val token: String) extends TelegramBot {

  override val baseURL: String = "https://api.telegram.org/"
  override val botURL: String = baseURL + "bot" + token

  private var updateRequest: HttpRequest = Http(botURL + "/getUpdates")
  private var lastUpdateId: Long = 0

  override def getUpdates(): List[Update] = {
    val json = updateRequest.param("offset", (lastUpdateId + 1).toString).asString

    val response = parse(json.body).extract[Response]
    assert(response.ok)

    val updatesList = response.result.getOrElse(List.empty)

    if (updatesList.nonEmpty)
      lastUpdateId = updatesList.map(_.update_id).max

    updatesList
  }

  override def sendMessage(chat_id: Int, text: String, reply_to_message_id: Option[Int], parse_mode: Option[String]): Boolean = {
    var sendRequest = Http(botURL + "/sendMessage").method("POST")

    def addParam(key: String)(value: Any): Unit = sendRequest = sendRequest.param(key, value.toString)

    addParam("chat_id")(chat_id)
    addParam("text")(text)

    reply_to_message_id.foreach(addParam("reply_to_message_id"))
    parse_mode.foreach(addParam("parse_mode"))

    val sendResult = sendRequest.asString
    val response = parse(sendResult.body).extract[Response]

    assert(response.ok)

    response.ok
  }
}
