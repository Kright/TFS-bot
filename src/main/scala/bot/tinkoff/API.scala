package bot.tinkoff

import org.json4s._
import org.json4s.native.JsonMethods._
import scalaj.http._

/**
  * Implementation of bot.tinkoff API
  *
  * TODO: Need to handle all the exceptions from Http library.
  */
trait TinkoffAPI {

  def authorizeAccount() = ???

  def getBalance() = ???

  def getHistory() = ???

  def getRates(): List[Rate]
}

object TinkoffAPI {

  def apply() = new TinkoffAPI {
    implicit val formats = DefaultFormats

    override def getRates() = {
      val json = Http("https://www.bot.tinkoff.ru/api/v1/currency_rates").asString
      val response = parse(json.body).extract[Response]

      assert(response.resultCode == "OK")

      //(jusual): I don't know which category I should use, so I picked a random one.
      response.payload.rates.filter(x => x.category == "DepositPayments" && x.toCurrency.name == "RUB")
    }
  }
}

case class Currency(code: Int, name: String)

case class LastUpdate(milliseconds: Long)

case class Payload(lastUpdate: LastUpdate, rates: List[Rate])

case class Response(resultCode: String, payload: Payload)

case class Rate(category: String,
                fromCurrency: Currency,
                toCurrency: Currency,
                buy: Double,
                sell: Option[Double])
