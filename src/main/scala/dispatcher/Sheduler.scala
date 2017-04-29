package dispatcher

import java.util.concurrent.{Executors, TimeUnit}

import scala.util.{Failure, Success, Try}

/**
  * Created by lgor on 4/15/17.
  */
object Sheduler {

  private val executor = Executors.newScheduledThreadPool(1)

  def apply(task: () => Unit, intervalMs: Long = 1000): Unit = {

    val runnable = new Runnable {
      override def run(): Unit = {

        Try(task()) match {
          case Failure(ex) => Console.err.println(s"Sheduler catched error : $ex")
          case _ => Unit
        }
      }
    }

    executor.scheduleAtFixedRate(runnable, 0, intervalMs, TimeUnit.MILLISECONDS)
  }

  def shutdownNow(): Unit = executor.shutdownNow()
}