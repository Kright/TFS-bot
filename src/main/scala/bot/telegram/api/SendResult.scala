package bot.telegram.api

import scala.util.{Failure, Success, Try}

/**
  * Created by lgor on 5/7/17.
  */
sealed trait SendResult

case object SendSuccess extends SendResult

case class SendFailed(reason: Response) extends SendResult

case class SendError(ex: Throwable) extends SendResult

object SendResult {
  def apply(response: => Response): SendResult =
    Try {
      if (response.ok) {
        SendSuccess
      } else {
        SendFailed(response)
      }
    } match {
      case Success(sendResult) => sendResult
      case Failure(error) => SendError(error)
    }
}
