package telegram

/**
  * Created by lgor on 4/15/17.
  */
object Runner {

  def apply(task: () => Unit, intervalMs: Long = 1000): Runner = {
    val r = new Runner(task, intervalMs)
    r.thread.start()
    r
  }
}

class Runner(val task: () => Unit, val intervalMs: Long) {

  @volatile
  private var running = true

  def isRunning = running

  def stop() = running = false

  private val thread = new Thread() {
    override def run(): Unit =
      while (running) {
        Thread.sleep(intervalMs)
        task()
      }
  }
}