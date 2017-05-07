package bot.telegram.api

/**
  * Created by lgor on 5/7/17.
  */
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
