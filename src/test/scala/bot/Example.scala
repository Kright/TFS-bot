package bot

import com.typesafe.config.ConfigFactory
import bot.Implicits.ConfigExt

/**
  * Created by lgor on 5/13/17.
  */
class LoadingFromConfigTest extends org.scalatest.FunSuite {

  test("test loading") {
    val loaded = ConfigFactory.load("propertyExample.conf") >> new TwoProperties
    assert(loaded.propertyName == "value")
    assert(loaded.anotherPropertyName == "anotherValue")
  }

  test("use not all from config") {
    val loaded = ConfigFactory.load("propertyExample.conf") >> new OnlyOneProperty
    assert(loaded.propertyName == "value")
  }

  test("missed propery exception") {
    intercept[com.typesafe.config.ConfigException.Missing] {
      val loaded = ConfigFactory.load("propertyExample.conf") >> new MissedProp
    }
  }
}

class TwoProperties() {
  var propertyName, anotherPropertyName = ""
}

class OnlyOneProperty() {
  var propertyName = ""
}

class MissedProp() {
  var missed = ""
}


