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

    for (update <- updatesList; if update.message.isDefined; message = update.message.get) {
        if (message.entities.isDefined)
          for (entity <- message.entities.get if entity.`type` == "bot_command") {
            // (jusual): message.text is defined because text contains name of bot_command
            val command = message.text.get.substring(entity.offset, entity.offset + entity.length)

            if (command == "/r" || command == "/rates") {
              val rates = getFormattedRates(getRates())
              sendMessage(message.chat.id, None, rates, Some("HTML"))
            }

            //TODO: Here could be your implementation of help, history and balance command

          }
    }
    Thread.sleep(1000)
  }
}
