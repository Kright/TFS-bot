package tinkoff

/**
  * Created by lgor on 4/15/17.
  */
class TinkoffAPI$Test extends org.scalatest.FunSuite {

  test("Api should return rates") {
    val rates = TinkoffAPI().getRates()
    assert(rates.nonEmpty)
  }

}
