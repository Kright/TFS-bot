package bot.dispatcher

import java.util.concurrent.{Executors, TimeUnit}

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

import scala.util.{Failure, Try}

/**
  * Created by lgor on 4/15/17.
  */
object Sheduler extends StrictLogging {

  val (threadsCount, delayBetweenExecutionMs) = {
    val config = ConfigFactory.load().getConfig("bot.scheduler")
    (config.getInt("threadsCount"), config.getInt("delayBetweenExecutionMs"))
  }

  @volatile
  private var running: Boolean = true

  private val executor = Executors.newScheduledThreadPool(threadsCount)

  def apply(task: () => Unit): Unit = {

    val runnable = new Runnable {
      override def run(): Unit = {

        Try(task()) match {
          case Failure(ex) => logger.error(s"scheduler catched exception: $ex")
          case _ =>
        }

        executor.schedule(this, delayBetweenExecutionMs, TimeUnit.MILLISECONDS)
      }
    }

    executor.submit(runnable)
  }

  def shutdownNow(): Unit = {
    running = false
    executor.shutdownNow()
  }
}