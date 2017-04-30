package bot.dispatcher

import java.util.concurrent.{Executors, TimeUnit}

import scala.util.{Failure, Success, Try}

/**
  * Created by lgor on 4/15/17.
  */
object Sheduler {

  val threadsCount = 1
  val delayBetweenExecutionMs = 1000

  @volatile
  private var running: Boolean = true

  private val executor = Executors.newScheduledThreadPool(threadsCount)

  def apply(task: () => Unit): Unit = {

    val runnable = new Runnable {
      override def run(): Unit = {

        Try(task()) match {
          case Failure(ex) => Console.err.println(s"Sheduler catched error : $ex")
          case _ => Unit
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