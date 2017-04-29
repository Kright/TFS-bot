package telegram

/**
  * Created by lgor on 4/16/17.
  */
trait Logging extends TelegramBot {

  abstract override def getUpdates(): List[Update] = {
    val upds: List[Update] = super.getUpdates()

    if (upds.nonEmpty)
      println(s"getUpdates: \n${upds.mkString("\n")}")

    upds
  }

  abstract override def sendMessage(chat_id: Int, text: String, reply_to_message_id: Option[Int], parse_mode: Option[String]): Boolean = {
    println(s"send message($chat_id, $text, $reply_to_message_id, $parse_mode)")
    super.sendMessage(chat_id, text, reply_to_message_id, parse_mode)
  }
}
