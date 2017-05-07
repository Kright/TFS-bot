package bot.telegram

import bot.telegram.api.{SendData, SendFailed, SendResult}

/**
  * Created by lgor on 4/16/17.
  */
trait Logging extends TelegramBot {

  abstract override def apply(sendData: SendData) = {
    println(s"sending : $sendData")
    val result = super.apply(sendData)
    log(result)

    result
  }

  private def log(sr: SendResult): Unit = sr match {
    case SendFailed(response) => println(s"sending failed: $response")
    case _ => Unit
  }
}
