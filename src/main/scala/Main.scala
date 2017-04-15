import dispatcher.Dispatcher
import telegram.{Runner, TelegramAPI}
import tinkoff.TinkoffAPI

import scala.io.Source

/**
  * Main polling loop.
  */
object Main extends App {

  val telegram = TelegramAPI(Source.fromFile("TelegramBotToken").getLines.mkString)
  val tinkoff = TinkoffAPI()

  val dispatcher = new Dispatcher(telegram, tinkoff)

  val runner = Runner(dispatcher.dispatch)

  val onExit = scala.io.StdIn.readLine()

  runner.stop()
  println("bot was stopped")
}
