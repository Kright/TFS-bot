package bot.telegram

import bot.telegram.api.{SendFailed, SendResult}

/**
  * Created by lgor on 4/16/17.
  */
trait Logging extends TelegramBot {

  abstract override def sendMessage(chatId: String,
                                    text: String,
                                    parseMode: Option[String],
                                    disableWebPagePreview: Boolean,
                                    disableNotification: Boolean,
                                    replyMessageId: Option[Int],
                                    replyMarkup: Option[String]): SendResult = {
    println(s"send message : $text")
    val result = super.sendMessage(chatId, text, parseMode, disableWebPagePreview, disableNotification, replyMessageId, replyMarkup)
    log(result)

    result
  }

  abstract override def sendSticker(chatId: String,
                                    fileId: String,
                                    disableNotification: Boolean,
                                    replyMessageId: Option[Int],
                                    replyMarkup: Option[String]): SendResult = {
    println(s"send sticker")
    val result = super.sendSticker(chatId, fileId, disableNotification, replyMessageId, replyMarkup)
    log(result)

    result
  }

  private def log(sr: SendResult): Unit = sr match {
    case SendFailed(response) => println(s"sending failed: $response")
    case _ => Unit
  }
}
