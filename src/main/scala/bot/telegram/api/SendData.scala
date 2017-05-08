package bot.telegram.api

import scalaj.http.HttpRequest

/**
  * Created by lgor on 5/7/17.
  */
sealed trait SendData {

  def putInto(httpRequest: HttpRequest): HttpRequest

  def methodName: String
}

case class SendMessage(chatId: String,
                       text: String,
                       parseMode: Option[String] = None,
                       disableWebPagePreview: Boolean = false,
                       disableNotification: Boolean = false,
                       replyMessageId: Option[Int] = None,
                       replyMarkup: Option[String] = None) extends SendData {

  def withText(newText: String): SendMessage = this.copy(text = newText)

  override def putInto(httpRequest: HttpRequest): HttpRequest = {
    var sendRequest = httpRequest

    def addParam(key: String)(value: Any): Unit = sendRequest = sendRequest.param(key, value.toString)

    addParam("chat_id")(chatId)
    addParam("text")(text)
    parseMode.foreach(addParam("parse_mode"))
    if (disableWebPagePreview) addParam("disable_web_page_preview")(disableWebPagePreview)
    if (disableNotification) addParam("disable_notification")(disableNotification)
    replyMessageId.foreach(addParam("reply_to_message_id"))
    replyMarkup.foreach(addParam("reply_markup"))

    sendRequest
  }

  override def methodName = "sendMessage"
}

case class SendSticker(chatId: String,
                       fileId: String,
                       disableNotification: Boolean = false,
                       replyMessageId: Option[Int] = None,
                       replyMarkup: Option[String] = None) extends SendData {

  override def putInto(httpRequest: HttpRequest): HttpRequest = {
    var sendRequest = httpRequest

    def addParam(key: String)(value: Any): Unit = sendRequest = sendRequest.param(key, value.toString)

    addParam("chat_id")(chatId)
    addParam("sticker")(fileId)
    if (disableNotification) addParam("disable_notification")(disableNotification)
    replyMessageId.foreach(addParam("reply_to_message_id"))
    replyMarkup.foreach(addParam("reply_markup"))

    sendRequest
  }

  override def methodName = "sendSticker"
}

case class answerCallbackQuery(callback_query_id: String,
                               text: Option[String] = None) extends SendData {

  override def putInto(httpRequest: HttpRequest): HttpRequest = {
    var sendRequest = httpRequest

    def addParam(key: String)(value: Any): Unit = sendRequest = sendRequest.param(key, value.toString)

    addParam("callback_query_id")(callback_query_id)
    text.foreach(addParam("text"))

    sendRequest
  }

  override def methodName = "answerCallbackQuery"
}