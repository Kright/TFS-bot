package bot.telegram

import bot.telegram.api._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

import scala.util.{Failure, Success, Try}

/**
  * Created by lgor on 4/16/17.
  */
trait TelegramLogging extends TelegramBot with StrictLogging {

  val (logSending, logRequestUpdates) = {
    val config = ConfigFactory.load().getConfig("bot.telegram.logging")
    (config.getBoolean("send"), config.getBoolean("requestUpdates"))
  }

  abstract override def requestUpdates(timeoutInSeconds: Int): List[Update] =
    if (!logRequestUpdates)
      super.requestUpdates(timeoutInSeconds)
    else
      Try {
        super.requestUpdates(timeoutInSeconds)
      } match {
        case Success(updates) => logger.debug(s"updates = {${updates.mkString(",\n")}}"); updates
        case Failure(exception) => logger.error(s"requestUpdates() error: $exception"); throw exception
      }

  abstract override def apply(sendData: SendData): SendResult =
    if (!logSending)
      super.apply(sendData)
    else {
      val result = super.apply(sendData)
      result match {
        case SendSuccess => logger.debug(s"sending: $sendData")
        case SendFailed(response) => logger.error(s"send failed: $response")
        case SendError(ex) => logger.error(s"send error: $ex")
      }
      result
    }

}
