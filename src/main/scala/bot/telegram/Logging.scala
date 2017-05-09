package bot.telegram

import bot.telegram.api._
import com.typesafe.scalalogging.{Logger, StrictLogging}
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Created by lgor on 4/16/17.
  */
trait TelegramUpdatesLogging extends TelegramBot with StrictLogging {

  abstract override def requestUpdates(timeoutInSeconds: Int): List[Update] =
    Try {
      super.requestUpdates(timeoutInSeconds)
    } match {
      case Success(updates) => logger.debug(s"updates = {${updates.mkString(",\n")}}"); updates
      case Failure(exception) => logger.error(s"requestUpdates() error: $exception"); throw exception
    }
}

trait TelegramSendLogging extends TelegramBot with StrictLogging {

  abstract override def apply(sendData: SendData): SendResult = {
    val result = super.apply(sendData)
    result match {
      case SendSuccess => logger.debug(s"sending: $sendData")
      case SendFailed(response) => logger.error(s"send failed: $response")
      case SendError(ex) => logger.error(s"send error: $ex")
    }
    result
  }
}