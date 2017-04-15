import dispatcher.Dispatcher
import telegram.{Sheduler, TelegramAPI}
import tinkoff.TinkoffAPI

import scala.io.Source

/**
  * Main polling loop.
  */
object Main extends App {

  val telegram = TelegramAPI(Source.fromFile("TelegramBotToken").getLines.mkString)
  val tinkoff = TinkoffAPI()

  val dispatcher = new Dispatcher(telegram, tinkoff)

  Sheduler(dispatcher.dispatch, 1000)

  val onExit = scala.io.StdIn.readLine()

  Sheduler.shutdownNow()
  println("bot was stopped")
}
