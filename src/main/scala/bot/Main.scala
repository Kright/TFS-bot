package bot

import bot.dispatcher.{Dispatcher, Sheduler}
import bot.telegram.TelegramBot
import bot.tinkoff.TinkoffAPI

import scala.io.Source

/**
  * Main polling loop.
  */
object Main extends App {

  val telegram = TelegramBot(Source.fromFile("TelegramBotToken").getLines.mkString)
  val tinkoff = TinkoffAPI()

  val dispatcher = new Dispatcher(telegram, tinkoff)

  Sheduler(dispatcher.dispatch, 1000)

  val onExit = scala.io.StdIn.readLine()

  Sheduler.shutdownNow()
  println("bot was stopped")
}
