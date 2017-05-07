package bot.dispatcher

import bot.telegram.TelegramBot
import bot.telegram.api.{SendMessage, _}
import bot.tinkoff.{Rate, TinkoffAPI}

/**
  * Created by lgor on 4/15/17.
  */
class Dispatcher(val bot: TelegramBot, val tinkoff: TinkoffAPI) {

  def dispatch(timeoutSeconds: Int = 0): Unit = {
    val updatesList = bot.requestUpdates(timeoutSeconds)

    for (update <- updatesList;
         message <- update.message;
         command <- message.botCommands) {
      processCommand(command, message)
    }
  }

  private def processCommand(cmd: String, msg: Message): Unit = cmd match {
    case "/r" | "/rates" => sendRates(msg)
    case _ => sendCommandUnknown(cmd, msg)
  }

  private def sendCommandUnknown(cmd: String, msg: Message): Unit = {
    bot(msg.chat.sendMessage withText s"unknown command : $cmd")
  }

  private def sendRates(msg: Message): Unit = {
    val rates = getFormattedRates(tinkoff.getRates())
    bot(SendMessage(msg.chat.id.toString, rates, parseMode = Option("HTML")))
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
