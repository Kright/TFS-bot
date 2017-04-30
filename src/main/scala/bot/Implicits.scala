package bot

/**
  * Created by lgor on 4/29/17.
  */
object Implicits {

  implicit class OptionStringBuilder(val s: StringBuilder) extends AnyVal {
    def push[T](name: String, option: Option[T]): StringBuilder = {
      option.foreach(o => s.append(s", $name = $o"))
      s
    }

    def push[T](option: Option[T]): StringBuilder = {
      option.foreach(o => s.append(s", $o"))
      s
    }
  }

}
