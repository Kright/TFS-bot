package bot.dispatcher

import java.util.concurrent.{Executors, TimeUnit}

import scala.util.{Failure, Success, Try}

/**
  * Created by lgor on 4/15/17.
  */
object Sheduler {

  @volatile
  private var running: Boolean = true

  private val executor = Executors.newCachedThreadPool()

  def apply(task: () => Unit): Unit = {

    val runnable = new Runnable {
      override def run(): Unit = {

        while (running) {
          Try(task()) match {
            case Failure(ex) => Console.err.println(s"Sheduler catched error : $ex")
            case _ => Unit
          }
          Thread.sleep(1000)
        }
      }
    }

    executor.submit(runnable)
  }

  def shutdownNow(): Unit = {
    running = false
    executor.shutdownNow()
  }
}