package bot.dispatcher

import bot.telegram.TelegramBot
import bot.telegram.api.{SendMessage, _}
import bot.tinkoff._
import java.time.format.{DateTimeFormatter, FormatStyle}
import java.time.{Instant, ZoneId, ZonedDateTime}
import scala.io.Source

/**
  * Created by lgor on 4/15/17.
  */
class Dispatcher(val bot: TelegramBot, val tinkoff: TinkoffAPI) {

  case class UserInfo(var lastMessageTime: Long,
                      var authorized: Boolean = false,
                      var sessionId: Option[String] = None,
                      var phone: Option[String] = None,
                      var operationTicket: Option[String] = None,
                      var reqCommand: Option[String] = None,
                      var reqPassword: Boolean = false,
                      var currentCode: String = "",
                      var currentPasswd: String = "")


  private val UserMap = scala.collection.mutable.Map[Long, UserInfo]()

  // перевести надписи на английский и собрать в файл?

  val contactInvite = "Для продолжения, предоставьте свой номер телефона."

  val passwordInvite = "Для дальнейшей авторизации необходимо ввести пароль."

  val smsInvite = "На Ваш номер было отправлено SMS-сообщение с кодом подтверждения.\nПожалуйста, введите его."

  val fiveMinReminder = "После 5 минут бездействия, Ваш сеанс авторизации был завершён"

  val tenMinReminder = "После 10 минут бездействия, Ваша сессия была завершена"

  val exitMessage = "Ваш сеанс работы с ботом завершён."

  val wrongPassword = "Введён неверный пароль.\nПопробуйте ещё раз."

  val wrongCode = "Введён неверный код подтверждения.\nПопробуйте ещё раз."

  val resendCode = "Код потверждения был отправлен повторно."

  val showPassword = "Чтобы увидеть введённые символы, нажмите на кнопку '???'"

  val codePanel = InlineKeyboardMarkup(List(
    List(InlineKeyboardButton("1"), InlineKeyboardButton("2"), InlineKeyboardButton("3")),
    List(InlineKeyboardButton("4"), InlineKeyboardButton("5"), InlineKeyboardButton("6")),
    List(InlineKeyboardButton("7"), InlineKeyboardButton("8"), InlineKeyboardButton("9")),
    List(InlineKeyboardButton("0"), InlineKeyboardButton("←"), InlineKeyboardButton("New code"))
  ))

  val passwordKeyboard1 = InlineKeyboardMarkup(List(
    List(InlineKeyboardButton("1"), InlineKeyboardButton("2"), InlineKeyboardButton("3"), InlineKeyboardButton("4"),
      InlineKeyboardButton("5"), InlineKeyboardButton("6"), InlineKeyboardButton("7"), InlineKeyboardButton("8")),
    List(InlineKeyboardButton("9"), InlineKeyboardButton("0"), InlineKeyboardButton("_"), InlineKeyboardButton("*"),
      InlineKeyboardButton("+"), InlineKeyboardButton("-"), InlineKeyboardButton("="), InlineKeyboardButton("←")),
    List(InlineKeyboardButton("q"), InlineKeyboardButton("w"), InlineKeyboardButton("e"), InlineKeyboardButton("r"),
      InlineKeyboardButton("t"), InlineKeyboardButton("y"), InlineKeyboardButton("u"), InlineKeyboardButton("i")),
    List(InlineKeyboardButton("o"), InlineKeyboardButton("p"), InlineKeyboardButton("a"), InlineKeyboardButton("s"),
      InlineKeyboardButton("d"), InlineKeyboardButton("f"), InlineKeyboardButton("g"), InlineKeyboardButton("h")),
    List(InlineKeyboardButton("j"), InlineKeyboardButton("k"), InlineKeyboardButton("l"), InlineKeyboardButton("z"),
      InlineKeyboardButton("x"), InlineKeyboardButton("c"), InlineKeyboardButton("v"), InlineKeyboardButton("b")),
    List(InlineKeyboardButton("n"), InlineKeyboardButton("m"), InlineKeyboardButton(","), InlineKeyboardButton("."),
      InlineKeyboardButton("/"), InlineKeyboardButton("\\"), InlineKeyboardButton("???"), InlineKeyboardButton("↲"))
  ))

  val passwordKeyboard2 = InlineKeyboardMarkup(List(
    List(InlineKeyboardButton("!"), InlineKeyboardButton("@"), InlineKeyboardButton("#"), InlineKeyboardButton("$"),
      InlineKeyboardButton("%"), InlineKeyboardButton("^"), InlineKeyboardButton("&"), InlineKeyboardButton("←")),
    List(InlineKeyboardButton("Q"), InlineKeyboardButton("W"), InlineKeyboardButton("E"), InlineKeyboardButton("R"),
      InlineKeyboardButton("T"), InlineKeyboardButton("Y"), InlineKeyboardButton("U"), InlineKeyboardButton("I")),
    List(InlineKeyboardButton("O"), InlineKeyboardButton("P"), InlineKeyboardButton("A"), InlineKeyboardButton("S"),
      InlineKeyboardButton("D"), InlineKeyboardButton("F"), InlineKeyboardButton("G"), InlineKeyboardButton("H")),
    List(InlineKeyboardButton("J"), InlineKeyboardButton("K"), InlineKeyboardButton("L"), InlineKeyboardButton("Z"),
      InlineKeyboardButton("X"), InlineKeyboardButton("C"), InlineKeyboardButton("V"), InlineKeyboardButton("B")),
    List(InlineKeyboardButton("N"), InlineKeyboardButton("M"), InlineKeyboardButton("?"), InlineKeyboardButton("~"),
      InlineKeyboardButton(":"), InlineKeyboardButton("`"), InlineKeyboardButton("???"), InlineKeyboardButton("↲"))
  ))

  val contactRequest = ReplyKeyboardMarkup(List(List(KeyboardButton("Предоставить", request_contact = true))))

  val helpText = Source.fromFile("help.txt").getLines.mkString("\n")

  def millisToDate(millis: Long): String = {
    val instant = Instant.ofEpochMilli(millis)
    val zonedDateTimeIst = ZonedDateTime.ofInstant(instant, ZoneId.of("Europe/Moscow"))
    zonedDateTimeIst.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))
  }

  private def executeSessionCommand(id: Long, command: String): Unit = {
    UserMap(id) match {
      case UserInfo(_, true, Some(s), Some(p), None, None, false, _, _) =>
        if (command == "history") {
          val history = tinkoff.getHistory(s)
          bot(SendMessage(id.toString, getFormattedHistory(history), parseMode = Option("HTML")))
        } else if (command == "balance") {
          val balance = tinkoff.getBalance(s)
          bot(SendMessage(id.toString, getFormattedBalance(balance), parseMode = Option("HTML")))
        }
      case UserInfo(_, false, Some(s), Some(p), None, None, false, _, _) =>
        UserMap(id).operationTicket = Some(tinkoff.sendAuthSMS(s, p))
        bot(SendMessage(id.toString, smsInvite, replyMarkup = Some(codePanel.toString)))
        UserMap(id).reqCommand = Some(command)
      case UserInfo(_, false, Some(s), None, None, None, false, _, _) =>
        bot(SendMessage(id.toString, contactInvite, replyMarkup = Some(contactRequest.toString)))
        UserMap(id).reqCommand = Some(command)
      case UserInfo(_, false, None, Some(p), None, None, false, _, _) =>
        val session = tinkoff.initSession()
        UserMap(id).sessionId = Some(session)
        UserMap(id).operationTicket = Some(tinkoff.sendAuthSMS(session, p))
        bot(SendMessage(id.toString, smsInvite, replyMarkup = Some(codePanel.toString)))
        UserMap(id).reqCommand = Some(command)
      case UserInfo(_, false, None, None, None, None, false, _, _) =>
        val session = tinkoff.initSession()
        UserMap(id).sessionId = Some(session)
        bot(SendMessage(id.toString, contactInvite, replyMarkup = Some(contactRequest.toString)))
        UserMap(id).reqCommand = Some(command)
      case _ =>
    }
  }

  def dispatch(timeoutSeconds: Int = 0): Unit = {
    val updatesList = bot.requestUpdates(timeoutSeconds)

    for (update <- updatesList) {
      update.callback_query.foreach(cbq => processCBQ(cbq))
      update.message.foreach(msg => processMessage(msg))
    }
  }

  private def processCBQ(cbq: CallbackQuery): Unit = {
    val id = cbq.message.chat.id
    UserMap(id) match {
      case UserInfo(_, _, Some(s), Some(p), Some(o), Some(c), _, curCode, _) =>
        val sym = cbq.data
        if (sym.forall(_.isDigit)) {
          val code = curCode + sym
          UserMap(id).currentCode = code
          if (code.length == 4) {
            tinkoff.confirmAuthSMS(s, o, code) match {
              case ConfirmResult(true, true) =>
                bot(SendMessage(id.toString, passwordInvite, replyMarkup = Some(passwordKeyboard1.toString)))
                bot(SendMessage(id.toString, showPassword, replyMarkup = Some(passwordKeyboard2.toString)))
                UserMap(id).currentCode = ""
                UserMap(id).operationTicket = None
                UserMap(id).reqPassword = true
                bot(answerCallbackQuery(cbq.id))
              case ConfirmResult(true, false) =>
                tinkoff.levelUp(s)
                UserMap(id).authorized = true
                UserMap(id).currentCode = ""
                UserMap(id).operationTicket = None
                if (c == "balance") bot(SendMessage(id.toString, getFormattedBalance(tinkoff.getBalance(s)), parseMode = Option("HTML")))
                else if (c == "history") bot(SendMessage(id.toString, getFormattedHistory(tinkoff.getHistory(s)), parseMode = Option("HTML")))
                UserMap(id).reqCommand = None
                bot(answerCallbackQuery(cbq.id))
              case _ =>
                UserMap(id).currentCode = ""
                bot(answerCallbackQuery(cbq.id, Some(wrongCode)))
            }
          }
          else bot(answerCallbackQuery(cbq.id))
        }
        else if (sym == "←") {
          val codeLen = UserMap(id).currentCode.length
          if (codeLen == 1)
            UserMap(id).currentCode = ""
          else if (codeLen > 1)
            UserMap(id).currentCode = curCode.substring(0, codeLen - 1)
          bot(answerCallbackQuery(cbq.id))
        }
        else if (sym == "New code") {
          UserMap(id).operationTicket = Some(tinkoff.sendAuthSMS(s, p))
          UserMap(id).currentCode = ""
          bot(answerCallbackQuery(cbq.id, Some(resendCode)))
        }
      case UserInfo(_, _, Some(s), Some(p), _, Some(c), true, _, curPasswd) =>
        val sym = cbq.data
        if (sym == "↲") {
          if (tinkoff.signUp(s, curPasswd)) {
            tinkoff.levelUp(s)
            UserMap(id).authorized = true
            UserMap(id).reqPassword = false
            UserMap(id).currentPasswd = ""
            bot(answerCallbackQuery(cbq.id))
            if (c == "balance") bot(SendMessage(id.toString, getFormattedBalance(tinkoff.getBalance(s)), parseMode = Option("HTML")))
            else if (c == "history") bot(SendMessage(id.toString, getFormattedHistory(tinkoff.getHistory(s)), parseMode = Option("HTML")))
            UserMap(id).reqCommand = None
          }
          else bot(answerCallbackQuery(cbq.id, Some(wrongPassword)))
        }
        else if (sym == "←") {
          val passLen = UserMap(id).currentPasswd.length
          if (passLen == 1)
            UserMap(id).currentPasswd = ""
          else if (passLen > 1)
            UserMap(id).currentPasswd = curPasswd.substring(0, passLen - 1)
          bot(answerCallbackQuery(cbq.id))
        }
        else if (sym == "???") {
          bot(answerCallbackQuery(cbq.id, Some(curPasswd)))
        }
        else {
          UserMap(id).currentPasswd = curPasswd + sym
          bot(answerCallbackQuery(cbq.id))
        }
      case _ => bot(answerCallbackQuery(cbq.id))
    }
  }

  private def processMessage(msg: Message): Unit = {
    val id = msg.chat.id
    if (UserMap contains id) {
      val timeDiff = Instant.now.getEpochSecond - UserMap(id).lastMessageTime
      if ((timeDiff >= 300) && (timeDiff <= 600)) {
        UserMap(id) match {
          case UserInfo(_, true, Some(s), _, _, _, _, _, _) =>
            tinkoff.signOut(s)
            UserMap(id).authorized = false
            UserMap(id).sessionId = None
            UserMap(id).operationTicket = None
            UserMap(id).reqCommand = None
            UserMap(id).reqPassword = false
            bot(SendMessage(id.toString, fiveMinReminder))
          case _ =>
        }
      }
      else if (timeDiff > 600) {
        UserMap(id) match {
          case UserInfo(_, _, Some(s), _, _, _, _, _, _) =>
            UserMap(id).authorized = false
            UserMap(id).sessionId = None
            UserMap(id).operationTicket = None
            UserMap(id).reqCommand = None
            UserMap(id).reqPassword = false
            bot(SendMessage(id.toString, tenMinReminder))
          case _ =>
        }
      }
      UserMap(id).lastMessageTime = msg.date
    }
    else
      UserMap += (id -> UserInfo(msg.date))

    UserMap(id) match {
      case UserInfo(_, _, _, None, None, None, false, _, _) =>
        msg.contact match {
          case Some(contact) if contact.user_id == msg.from.get.id => UserMap(id).phone = Some(contact.phone_number)
          case None =>
        }
      case UserInfo(_, _, Some(s), None, None, Some(c), false, _, _) =>
        msg.contact match {
          case Some(contact) =>
            val phone = contact.phone_number
            UserMap(id).phone = Some(phone)
            UserMap(id).operationTicket = Some(tinkoff.sendAuthSMS(s, phone))
            bot(SendMessage(id.toString, smsInvite, replyMarkup = Some(codePanel.toString)))
          case None =>
        }
      case UserInfo(_, false, Some(s), Some(p), None, Some(c), false, _, _) =>
        UserMap(id).operationTicket = Some(tinkoff.sendAuthSMS(s, p))
        bot(SendMessage(id.toString, smsInvite, replyMarkup = Some(codePanel.toString)))
      case _ =>
    }

    msg.botCommands.foreach(cmd => processCommand(cmd, msg))
  }

  private def processCommand(cmd: String, msg: Message): Unit = cmd match {
    case "/h" | "/help" | "/start" => bot(SendMessage(msg.chat.id.toString, helpText))
    case "/b" | "/balance" => executeSessionCommand(msg.chat.id, "balance")
    case "/hi" | "/history" => executeSessionCommand(msg.chat.id, "history")
    case "/e" | "/end" => endBotSession(msg)
    case "/r" | "/rates" => sendRates(msg)
    case _ => sendCommandUnknown(cmd, msg)
  }

  private def endBotSession(msg: Message): Unit = {
    val id = msg.chat.id
    UserMap(id) match {
      case UserInfo(_, true, Some(s), _, _, _, _, _, _) =>
        tinkoff.signOut(s)
        UserMap(id).authorized = false
        UserMap(id).sessionId = None
        UserMap(id).phone = None
        UserMap(id).operationTicket = None
        UserMap(id).reqCommand = None
        UserMap(id).reqPassword = false
      case UserInfo(_, false, _, _, _, _, _, _, _) =>
        UserMap(id).sessionId = None
        UserMap(id).phone = None
        UserMap(id).operationTicket = None
        UserMap(id).reqCommand = None
        UserMap(id).reqPassword = false
      case _ =>
    }
    bot(SendMessage(id.toString, exitMessage))
  }

  private def sendCommandUnknown(cmd: String, msg: Message): Unit = {
    bot(msg.chat.sendMessage withText s"unknown command : $cmd")
  }

  private def sendRates(msg: Message): Unit = {
    val session = UserMap(msg.chat.id).sessionId
    val rates = getFormattedRates(tinkoff.getRates(session))
    bot(SendMessage(msg.chat.id.toString, rates, parseMode = Option("HTML")))
  }

  private def getFormattedRates(rates: List[Rate]): String = {
    val text = rates.map(x => "%3s: %7.3f  %7.3f".format(x.fromCurrency.name, x.buy, x.sell.get)).mkString("\n")

    "<pre>" +
      s"""       Buy      Sell
         |      ------   ------
         |$text</pre>
      """.stripMargin
  }

  private def getFormattedBalance(accounts: List[Account]): String = {
    val cardsBalanceList = accounts.map(x => "%s: %7.3f %3s".format(x.name, x.moneyAmount.value, x.moneyAmount.currency.name))
    "<pre>" + cardsBalanceList.mkString("\n") + "</pre>"
  }

  private def getFormattedHistory(accounts: List[Operation]): String = {
    if (accounts.isEmpty) "Записей об операциях нет."
    else {
      val operationsList = accounts.map(x => "Дата: %s\nКатегория: %s\nОписание: %s\nСумма: %s%7.3f %3s\n".format(
        millisToDate(x.operationTime.milliseconds), x.category.name, x.description, if (x.`type` == "Credit") "+" else "-", x.amount.value, x.amount.currency.name
      ))
      "<pre>" + operationsList.mkString("\n\n") + "</pre>"
    }
  }
}
