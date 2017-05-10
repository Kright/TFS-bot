package bot.telegram

import bot.telegram.api._
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods.parse
import com.typesafe.config._

import scalaj.http.Http

/**
  * Created by lgor on 4/27/17.
  */
class TelegramBotImpl(val token: String,
                      private val conf: Config = ConfigFactory.load().getConfig("bot.telegram.connection")) extends TelegramBot {

  var lastUpdatedId: Int = -1

  val httpUpdate = Http(s"$url/getUpdates")

  implicit val formats = DefaultFormats

  private val connTimeoutMs = conf.getInt("connTimeoutMs")
  private val readTimeoutMs = conf.getInt("readTimeoutMs")
  private val additionalTimeoutMs = conf.getInt("additionalTimeoutMs")

  override def apply(sendData: SendData): SendResult = {
    val httpRequest = sendData.putInto(Http(s"$url/${sendData.methodName}").method("POST"))

    SendResult {
      parse(httpRequest.asString.body).extract[api.Response]
    }
  }

  override def requestUpdates(timeoutSeconds: Int = 0): List[Update] = {

    var request = httpUpdate

    request =
      if (timeoutSeconds != 0) {
        request.
          timeout(connTimeoutMs, readTimeoutMs + 1000 * timeoutSeconds + additionalTimeoutMs).
          param("timeout", timeoutSeconds.toString)
      } else {
        request.timeout(connTimeoutMs, readTimeoutMs)
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
}

