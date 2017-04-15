package dispatcher

import scala.io.Source

import telegram.API._
import tinkoff.API._

/**
  * Main polling loop.
  */
object Dispatcher extends App {

  def getFormattedRates(rates: List[Rate]): String = {
    val textRatesList = rates.map(x => "%3s: %7.3f  %7.3f".format(x.fromCurrency.name, x.buy, x.sell.get))
    "<pre>" +
      "       Buy      Sell\n" +
      "      ------   ------\n" +
      textRatesList.mkString("\n") + "</pre>"
  }

  setToken(Source.fromFile("TelegramBotToken").getLines.mkString)

  while (true) {
    val updatesList = getUpdates()
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
          val rates = getFormattedRates(getRates())
          sendMessage(message.chat.id, None, rates, Some("HTML"))
        }

      //TODO: Here could be your implementation of help, history and balance command
    }
    Thread.sleep(1000)
  }
}
