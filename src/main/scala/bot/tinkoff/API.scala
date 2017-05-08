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

  def initSession(): String

  def signOut(sessionId: String)

  def signUp(sessionId: String, password: String): Boolean

  def sendAuthSMS(sessionId: String, phone: String): String

  def confirmAuthSMS(sessionId: String, operationTicket: String, SMS: String): ConfirmResult

  def levelUp(sessionId: String)

  def getBalance(sessionId: String): List[Account]

  def getHistory(sessionId: String): List[Operation]

  def getRates(sessionId: Option[String] = None): List[Rate]
}

object TinkoffAPI {

  def apply() = new TinkoffAPI {
    implicit val formats = DefaultFormats

    val URL = "https://www.tinkoff.ru/api/v1/"
    val origin = "web,ib5,platform"

    override def initSession(): String = {
      val sessionRequest = Http(URL + "session").param("origin", origin).asString
      parse(sessionRequest.body).extract[Session].payload
    }

    override def signOut(sessionId: String): Unit = {
      val signoutRequset = Http(URL + "sign_out").method("POST").param("origin", origin).param("sessionid", sessionId).asString
    }

    override def signUp(sessionId: String, password: String): Boolean = {
      val signupRequset = Http(URL + "sign_up").method("POST").param("origin", origin).param("sessionid", sessionId).param("password", password).asString
      parse(signupRequset.body).extract[Result].resultCode == "OK"
    }

    override def sendAuthSMS(sessionId: String, phone: String): String = {
      val authRequest = Http(URL + "sign_up").method("POST").param("origin", origin).param("sessionid", sessionId).param("phone", phone).asString
      parse(authRequest.body).extract[SignUp].operationTicket
    }

    override def confirmAuthSMS(sessionId: String, operationTicket: String, SMS: String): ConfirmResult = {
      val confirmRequest = Http(URL + "confirm").method("POST").param("origin", origin)
        .param("sessionid", sessionId).param("initialOperationTicket", operationTicket)
        .param("initialOperation", "sign_up").param("confirmationData", "{\"SMSBYID\":\"" + SMS + "\"}").asString
      val confirmResponse = parse(confirmRequest.body).extract[Result]
      if (confirmResponse.resultCode != "OK") ConfirmResult(succeed = false, needPassword = false)
      else {
        val confirmInfo = parse(confirmRequest.body).extract[Confirm]
        ConfirmResult(succeed = true, needPassword = confirmInfo.payload.additionalAuth.needPassword)
      }
    }

    override def levelUp(sessionId: String): Unit = {
      val levelupRequset = Http(URL + "level_up").method("POST").param("origin", origin).param("sessionid", sessionId).asString
    }

    override def getBalance(sessionId: String): List[Account] = {
      val balanceRequest = Http(URL + "accounts_flat").param("origin", origin).param("sessionid", sessionId).asString
      parse(balanceRequest.body).extract[Accounts].payload
    }

    override def getHistory(sessionId: String): List[Operation] = {
      val yearAgo = (java.time.Instant.now.getEpochSecond - 31536000) * 1000
      val historyRequest = Http(URL + "operations").param("origin", origin).param("sessionid", sessionId).param("start", yearAgo.toString).asString
      parse(historyRequest.body).extract[History].payload take 10
    }

    override def getRates(sessionId: Option[String] = None): List[Rate] = {
      var ratesRequest = Http(URL + "currency_rates").param("origin", origin)
      sessionId.foreach(id => ratesRequest = ratesRequest.param("sessionid", id))
      val ratesResponse = parse(ratesRequest.asString.body).extract[RatesResponse]

      //TODO: Exception raising in case of error (response.resultCode != "OK")

      //(jusual): I don't know which category I should use, so I picked a random one.
      ratesResponse.payload.rates.filter(x => x.category == "DepositPayments" && x.toCurrency.name == "RUB")
    }
  }
}

case class Account(name: String, moneyAmount: Amount, cardNumbers: List[Card])

case class Accounts(resultCode: String, payload: List[Account])

case class AdditionalAuth(needLogin: Boolean, needPassword: Boolean, needRegister: Boolean)

case class Amount(currency: Currency, value: Double)

case class Card(name: String, availableBalance: Amount)

case class Confirm(resultCode: String, payload: ConfirmPayload)

case class ConfirmPayload(accessLevel: String, additionalAuth: AdditionalAuth)

case class ConfirmResult(succeed: Boolean, needPassword: Boolean)

case class Currency(code: Int, name: String)

case class OperationTime(milliseconds: Long)

case class History(resultCode: String, payload: List[Operation])

case class Operation(description: String, amount: Amount, operationTime: OperationTime, category: OperationCategory, `type`: String)

case class OperationCategory(id: String, name: String)

case class Rate(category: String,
                fromCurrency: Currency,
                toCurrency: Currency,
                buy: Double,
                sell: Option[Double])

case class RatesPayload(lastUpdate: OperationTime, rates: List[Rate])

case class RatesResponse(resultCode: String, payload: RatesPayload)

case class Result(resultCode: String)

case class Session(resultCode: String, payload: String)

case class SignUp(resultCode: String, operationTicket: String)
