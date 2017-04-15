package telegram

import java.util.concurrent.{Executors, TimeUnit}

/**
  * Created by lgor on 4/15/17.
  */
object Sheduler {

  private val executor = Executors.newScheduledThreadPool(1)

  def apply(task: () => Unit, intervalMs: Long = 1000): Unit = {
    val runnable = new Runnable {
      override def run() = task()
    }

    executor.scheduleAtFixedRate(runnable, 0, intervalMs, TimeUnit.MILLISECONDS)
  }

  def shutdownNow() = executor.shutdownNow()
}