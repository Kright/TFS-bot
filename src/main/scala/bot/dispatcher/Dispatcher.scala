package bot.dispatcher

import bot.telegram.TelegramBot
import bot.telegram.api._
import bot.tinkoff.{Rate, TinkoffAPI}

/**
  * Created by lgor on 4/15/17.
  */
class Dispatcher(val telegram: TelegramBot, val tinkoff: TinkoffAPI) {

  def dispatch(): Unit = {
    val updatesList = telegram.getUpdates()

    for (update <- updatesList;
         message <- update.message;
         command <- message.entity("bot_command")) {
      processCommand(command, message)
    }
  }

  private def processCommand(cmd: String, msg: Message): Unit = cmd match {
    case "/r" | "/rates" => sendRates(msg)
    case _ => sendCommandUnknown(cmd, msg)
  }

  private def sendCommandUnknown(cmd: String, msg: Message): Unit = {
    telegram.sendMessage(msg.chat.id, s"unknown command : $cmd", parse_mode = Option("HTML"))
  }

  private def sendRates(msg: Message): Unit = {
    val rates = getFormattedRates(tinkoff.getRates())
    telegram.sendMessage(msg.chat.id, rates, parse_mode = Option("HTML"))
  }

  private def getFormattedRates(rates: List[Rate]): String = {
    val text = rates.map(x => "%3s: %7.3f  %7.3f".format(x.fromCurrency.name, x.buy, x.sell.get)).mkString("\n")

    "<pre>" +
      s"""       Buy      Sell
         |      ------   ------
         |$text</pre>
      """.stripMargin
  }
}
