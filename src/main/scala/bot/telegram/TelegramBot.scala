package bot.telegram

import bot.telegram.api.{SendResult, Update}

/**
  * Implementation of bot.telegram bot API.
  * https://core.bot.telegram.org/bots/api
  */

trait TelegramBot {

  val token: String

  def requestUpdates(timeoutInSeconds: Int = 0): List[Update]

  def sendMessage(chatId: String,
                  text: String,
                  parseMode: Option[String] = None,
                  disableWebPagePreview: Boolean = false,
                  disableNotification: Boolean = false,
                  replyMessageId: Option[Int] = None,
                  replyMarkup: Option[String] = None): SendResult

  def sendSticker(chatId: String,
                  fileId: String,
                  disableNotification: Boolean = false,
                  replyMessageId: Option[Int] = None,
                  replyMarkup: Option[String] = None): SendResult

  def sendMessage(chatId: Long, text: String): SendResult = sendMessage(chatId.toString, text)

  def url: String = s"https://api.telegram.org/bot$token"
}

object TelegramBot {

  sealed trait ParseMode

  def apply(token: String): TelegramBot = new BotImpl(token) with Logging
}


