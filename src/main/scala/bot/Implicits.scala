package bot

import com.typesafe.config.Config

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

  implicit class ConfigExt(val conf: Config) extends AnyVal {

    def >>[T](simpleClass: T): T = {
      simpleClass.getClass.getDeclaredFields.foreach { f =>
        val defaultAccess = f.isAccessible
        f.setAccessible(true)
        f.set(simpleClass, conf.getString(f.getName))
        f.setAccessible(defaultAccess)
      }
      simpleClass
    }
  }

}
