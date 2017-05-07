package bot.telegram

import bot.telegram.api._
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods.parse

import scalaj.http.Http

/**
  * Created by lgor on 4/27/17.
  */
class TelegramBotImpl(val token: String) extends TelegramBot {

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

    synchronized {
      if (lastUpdatedId != -1)
        request = request.param("offset", s"${lastUpdatedId + 1}")

      val result = request.asString
      val response = parse(result.body).extract[api.Response]

      assert(response.ok, response)

      val updates = response.result.getOrElse(List.empty)

      if (updates.nonEmpty)
        lastUpdatedId = updates.map(_.update_id).max

      assert(updates.forall(u => u.message.forall(_.isValid)))

      updates
    }
  }

  override def apply(sendData: SendData): SendResult = {
    val httpRequest = sendData.putInto(Http(s"$url/${sendData.methodName}").method("POST"))

    val sendResult = httpRequest.asString
    val response = parse(sendResult.body).extract[api.Response]

    SendResult(response)
  }
}

