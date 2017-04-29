package bot.telegram

import bot.telegram.api.{SendResult, Update}
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods.parse

import scalaj.http.Http

/**
  * Created by lgor on 4/27/17.
  */
class BotImpl(val token: String) extends TelegramBot {

  var lastUpdatedId: Int = -1

  val httpUpdate = Http(s"$url/getUpdates")
  val httpSendMessage = Http(s"$url/sendMessage").method("POST")
  val httpSendSticker = Http(s"$url/sendSticker").method("POST")

  implicit val formats = DefaultFormats

  override def requestUpdates(timeoutSeconds: Int = 0): List[Update] = {
    var request = httpUpdate

    if (timeoutSeconds != 0) {
      request = request.
        timeout(1000, 5000 + 1000 * timeoutSeconds).
        param("timeout", timeoutSeconds.toString)
    }

    if (lastUpdatedId != -1)
      request = request.param("offset", s"${lastUpdatedId + 1}")

    val result = request.asString
    val response = parse(result.body).extract[api.Response]

    assert(response.ok, response)

    val updates = response.result.getOrElse(List.empty)

    if (updates.nonEmpty)
      lastUpdatedId = updates.map(_.update_id).max

    updates
  }

  override def sendMessage(chatId: String,
                           text: String,
                           parseMode: Option[String],
                           disableWebPagePreview: Boolean,
                           disableNotification: Boolean,
                           replyMessageId: Option[Int],
                           replyMarkup: Option[String]): SendResult = {

    var sendRequest = httpSendMessage

    def addParam(key: String)(value: Any): Unit = sendRequest = sendRequest.param(key, value.toString)

    addParam("chat_id")(chatId)
    addParam("text")(text)
    parseMode.foreach(addParam("parse_mode"))
    if (disableWebPagePreview) addParam("disable_web_page_preview")(disableWebPagePreview)
    if (disableNotification) addParam("disable_notification")(disableNotification)
    replyMessageId.foreach(addParam("reply_to_message_id"))
    replyMarkup.foreach(addParam("reply_markup"))

    val sendResult = sendRequest.asString
    val response = parse(sendResult.body).extract[api.Response]

    SendResult(response)
  }

  override def sendSticker(chatId: String, fileId: String, disableNotification: Boolean, replyMessageId: Option[Int], replyMarkup: Option[String]): SendResult = {
    var sendRequest = httpSendSticker

    def addParam(key: String)(value: Any): Unit = sendRequest = sendRequest.param(key, value.toString)

    addParam("chat_id")(chatId)
    addParam("sticker")(fileId)
    if (disableNotification) addParam("disable_notification")(disableNotification)
    replyMessageId.foreach(addParam("reply_to_message_id"))
    replyMarkup.foreach(addParam("reply_markup"))

    val sendResult = sendRequest.asString
    val response = parse(sendResult.body).extract[api.Response]

    SendResult(response)
  }
}

