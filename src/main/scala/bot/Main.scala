package bot

import bot.dispatcher.{Dispatcher, Sheduler}
import bot.telegram.TelegramBot
import bot.tinkoff.TinkoffAPI

import com.typesafe.config.ConfigFactory

/**
  * Main polling loop.
  */
object Main extends App {

  val token = ConfigFactory.load("token").getConfig("bot.telegram").getString("token")
  val telegram = TelegramBot(token)
  val tinkoff = TinkoffAPI()

  val dispatcher = new Dispatcher(telegram, tinkoff)

  Sheduler(() => dispatcher.dispatch(60))

  val onExit = scala.io.StdIn.readLine()

  Sheduler.shutdownNow()
  println("bot will be stopped")
}
