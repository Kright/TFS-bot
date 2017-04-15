package dispatcher

import telegram.TelegramAPI

import tinkoff.{Rate, TinkoffAPI}

/**
  * Created by lgor on 4/15/17.
  */
class Dispatcher(val telegram: TelegramAPI, val tinkoff: TinkoffAPI) {

  def dispatch(): Unit = {
    val updatesList = telegram.getUpdates()

    if (updatesList.nonEmpty)
      println(updatesList)

    for (update <- updatesList;
         message <- update.message;
         msgEntities <- message.entities;
         entity <- msgEntities if entity.`type` == "bot_command"
    ) {
      // (jusual): message.text is defined because text contains name of bot_command

      message.text.
        map(_.substring(entity.offset, entity.offset + entity.length)).
        filter(cmd => cmd == "/r" || cmd == "/rates").
        foreach { cmd =>
          val rates = getFormattedRates(tinkoff.getRates())
          telegram.sendMessage(message.chat.id, rates, parse_mode = Option("HTML"))
        }

      //TODO: Here could be your implementation of help, history and balance commandF
    }
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
