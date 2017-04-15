import dispatcher.Dispatcher
import telegram.TelegramAPI
import tinkoff.TinkoffAPI

import scala.io.Source

/**
  * Main polling loop.
  */
object Main extends App {

  val telegram = TelegramAPI(Source.fromFile("TelegramBotToken").getLines.mkString)
  val tinkoff = TinkoffAPI()

  val dispatcher = new Dispatcher(telegram, tinkoff)

  while (true) {
    dispatcher.dispatch()
    Thread.sleep(1000)
  }
}
