package bot.dispatcher

import bot.telegram.TelegramBot
import bot.telegram.api.{SendMessage, _}
import bot.tinkoff._
import java.time.format.{DateTimeFormatter, FormatStyle}
import java.time.{Instant, ZoneId, ZonedDateTime}
import scala.util.Random
import com.typesafe.config.ConfigFactory
import bot.Implicits.ConfigExt
import collection.JavaConverters._

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

  private val userMap = scala.collection.mutable.Map[Long, UserInfo]()

  val contentConfig = ConfigFactory.load("content").getConfig("bot")

  val text = contentConfig.getConfig("messages") >> new Text

  val tinkoffStickers = contentConfig.getStringList("stickers").asScala.toArray

  val codePanel = InlineKeyboardMarkup(List(
    List("1", "2", "3"),
    List("4", "5", "6"),
    List("7", "8", "9"),
    List("0", "←", "New code")).
    map(_.map(InlineKeyboardButton(_)))
  )

  val passwordKeyboard1 = InlineKeyboardMarkup(List(
    List("1", "2", "3", "4", "5", "6", "7", "8"),
    List("9", "0", "_", "*", "+", "-", "=", "←"),
    List("q", "w", "e", "r", "t", "y", "u", "i"),
    List("o", "p", "a", "s", "d", "f", "g", "h"),
    List("j", "k", "l", "z", "x", "c", "v", "b"),
    List("n", "m", ",", ".", "/", "\\", "???", "↲")).
    map(_.map(InlineKeyboardButton(_)))
  )

  val passwordKeyboard2 = InlineKeyboardMarkup(List(
    List("!", "@", "#", "$", "%", "^", "&", "←"),
    List("Q", "W", "E", "R", "T", "Y", "U", "←I"),
    List("O", "P", "A", "S", "D", "F", "G", "H"),
    List("J", "K", "L", "Z", "X", "C", "V", "B"),
    List("N", "M", "?", "~", ":", "`", "???", "↲")).
    map(_.map(InlineKeyboardButton(_)))
  )

  val contactRequest = ReplyKeyboardMarkup(List(List(KeyboardButton(text.shareContact, request_contact = true))))

  def millisToDate(millis: Long): String = {
    val instant = Instant.ofEpochMilli(millis)
    val zonedDateTimeIst = ZonedDateTime.ofInstant(instant, ZoneId.of("Europe/Moscow"))
    zonedDateTimeIst.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))
  }

  def dispatch(timeoutSeconds: Int = 0): Unit = {
    val updatesList = bot.requestUpdates(timeoutSeconds)

    for (update <- updatesList) {
      update.callback_query.foreach(cbq => processCBQ(cbq))
      update.message.foreach(msg => processMessage(msg))
    }
  }

  private def processSessionCommand(id: Long, command: String): Unit = {
    userMap(id) match {
      case UserInfo(_, true, Some(session), Some(phone), None, None, false, _, _) =>
        executeSessionCommand(id, session, command)
      case UserInfo(_, false, Some(session), Some(phone), None, None, false, _, _) =>
        userMap(id).operationTicket = Some(tinkoff.sendAuthSMS(session, phone))
        bot(SendMessage(id.toString, text.smsInvite, replyMarkup = Some(codePanel.toString)))
        userMap(id).reqCommand = Some(command)
      case UserInfo(_, false, Some(session), None, None, None, false, _, _) =>
        bot(SendMessage(id.toString, text.contactInvite, replyMarkup = Some(contactRequest.toString)))
        userMap(id).reqCommand = Some(command)
      case UserInfo(_, false, None, Some(phone), None, None, false, _, _) =>
        val session = tinkoff.initSession()
        userMap(id).sessionId = Some(session)
        userMap(id).operationTicket = Some(tinkoff.sendAuthSMS(session, phone))
        bot(SendMessage(id.toString, text.smsInvite, replyMarkup = Some(codePanel.toString)))
        userMap(id).reqCommand = Some(command)
      case UserInfo(_, false, None, None, None, None, false, _, _) =>
        val session = tinkoff.initSession()
        userMap(id).sessionId = Some(session)
        bot(SendMessage(id.toString, text.contactInvite, replyMarkup = Some(contactRequest.toString)))
        userMap(id).reqCommand = Some(command)
      case _ =>
    }
  }

  private def processCBQ(cbq: CallbackQuery): Unit = if (cbq.message.chat.`type` == "private") {
    val id = cbq.message.chat.id
    userMap(id) match {
      case UserInfo(_, _, Some(session), Some(phone), Some(opTicket), Some(command), false, curCode, _) =>
        cbq.data match {
          case sym: String if sym.forall(_.isDigit) =>
            val code = curCode + sym
            userMap(id).currentCode = code
            if (code.length == 4) {
              tinkoff.confirmAuthSMS(session, opTicket, code) match {
                case ConfirmResult(true, true) =>
                  bot(SendMessage(id.toString, text.passwordInvite, replyMarkup = Some(passwordKeyboard1.toString)))
                  bot(SendMessage(id.toString, text.showPassword, replyMarkup = Some(passwordKeyboard2.toString)))
                  userMap(id).currentCode = ""
                  userMap(id).operationTicket = None
                  userMap(id).reqPassword = true
                  bot(AnswerCallbackQuery(cbq.id))
                case ConfirmResult(true, false) =>
                  tinkoff.levelUp(session)
                  userMap(id).authorized = true
                  userMap(id).currentCode = ""
                  userMap(id).operationTicket = None
                  executeSessionCommand(id, session, command)
                  userMap(id).reqCommand = None
                  bot(AnswerCallbackQuery(cbq.id))
                case _ =>
                  userMap(id).currentCode = ""
                  bot(AnswerCallbackQuery(cbq.id, Some(text.wrongCode)))
              }
            }
            else bot(AnswerCallbackQuery(cbq.id))
          case "←" =>
            val codeLen = userMap(id).currentCode.length
            if (codeLen > 1) userMap(id).currentCode = curCode.substring(0, codeLen - 1)
            else userMap(id).currentCode = ""
            bot(AnswerCallbackQuery(cbq.id))
          case "New code" =>
            userMap(id).operationTicket = Some(tinkoff.sendAuthSMS(session, phone))
            userMap(id).currentCode = ""
            bot(AnswerCallbackQuery(cbq.id, Some(text.resendCode)))
          case _ => bot(AnswerCallbackQuery(cbq.id))
        }
      case UserInfo(_, _, Some(session), Some(phone), _, Some(command), true, _, curPasswd) =>
        cbq.data match {
          case "↲" =>
            if (tinkoff.signUp(session, curPasswd)) {
              tinkoff.levelUp(session)
              userMap(id).authorized = true
              userMap(id).reqPassword = false
              userMap(id).currentPasswd = ""
              bot(AnswerCallbackQuery(cbq.id))
              executeSessionCommand(id, session, command)
              userMap(id).reqCommand = None
            }
            else bot(AnswerCallbackQuery(cbq.id, Some(text.wrongPassword)))
          case "←" =>
            val passLen = userMap(id).currentPasswd.length
            if (passLen > 1) userMap(id).currentPasswd = curPasswd.substring(0, passLen - 1)
            else userMap(id).currentPasswd = ""
            bot(AnswerCallbackQuery(cbq.id))
          case "???" =>
            bot(AnswerCallbackQuery(cbq.id, Some(curPasswd)))
          case sym: String if sym.length == 1 =>
            userMap(id).currentPasswd = curPasswd + sym
            bot(AnswerCallbackQuery(cbq.id))
          case _ => bot(AnswerCallbackQuery(cbq.id))
        }
      case _ => bot(AnswerCallbackQuery(cbq.id))
    }
  }

  private def processMessage(msg: Message): Unit = if (msg.chat.`type` == "private") {
    val id = msg.chat.id
    if (userMap contains id) {
      val timeDiff = Instant.now.getEpochSecond - userMap(id).lastMessageTime
      if ((timeDiff >= 300) && (timeDiff <= 600)) {
        endBotSession(id)
        bot(SendMessage(id.toString, text.fiveMinReminder))
      }
      else if (timeDiff > 600) {
        endBotSession(id)
        bot(SendMessage(id.toString, text.tenMinReminder))
      }
      userMap(id).lastMessageTime = msg.date
    }
    else
      userMap += (id -> UserInfo(msg.date))

    userMap(id) match {
      case UserInfo(_, _, _, None, None, None, false, _, _) =>
        msg.contact match {
          case Some(contact) if contact.user_id == msg.from.get.id => userMap(id).phone = Some(contact.phone_number)
          case None =>
        }
      case UserInfo(_, _, Some(session), None, None, Some(command), false, _, _) =>
        msg.contact match {
          case Some(contact) =>
            val phone = contact.phone_number
            userMap(id).phone = Some(phone)
            userMap(id).operationTicket = Some(tinkoff.sendAuthSMS(session, phone))
            bot(SendMessage(id.toString, text.smsInvite, replyMarkup = Some(codePanel.toString)))
          case None =>
        }
      case UserInfo(_, false, Some(session), Some(phone), None, Some(command), false, _, _) =>
        userMap(id).operationTicket = Some(tinkoff.sendAuthSMS(session, phone))
        bot(SendMessage(id.toString, text.smsInvite, replyMarkup = Some(codePanel.toString)))
      case _ =>
    }

    msg.botCommands.foreach(cmd => processCommand(cmd, msg))
  }

  private def processCommand(cmd: String, msg: Message): Unit = cmd match {
    case "/h" | "/help" | "/start" => bot(SendMessage(msg.chat.id.toString, text.helpText))
    case "/b" | "/balance" => processSessionCommand(msg.chat.id, "balance")
    case "/hi" | "/history" => processSessionCommand(msg.chat.id, "history")
    case "/e" | "/end" => endBotSession(msg.chat.id); bot(SendMessage(msg.chat.id.toString, text.exitMessage))
    case "/r" | "/rates" => sendRates(msg)
    case "/s" | "/sticker" => sendTinkoffSticker(msg.chat.id)
    case _ => sendCommandUnknown(cmd, msg)
  }

  private def executeSessionCommand(id: Long, session: String, command: String): Unit = command match {
    case "history" =>
      val history = tinkoff.getHistory(session)
      bot(SendMessage(id.toString, getFormattedHistory(history), parseMode = Option("HTML")))
    case "balance" =>
      val balance = tinkoff.getBalance(session)
      bot(SendMessage(id.toString, getFormattedBalance(balance), parseMode = Option("HTML")))
    case _ =>
  }

  private def endBotSession(id: Long): Unit = {
    userMap(id).sessionId.foreach(session => tinkoff.signOut(session))
    userMap(id).authorized = false
    userMap(id).sessionId = None
    userMap(id).phone = None
    userMap(id).operationTicket = None
    userMap(id).reqCommand = None
    userMap(id).reqPassword = false
  }

  private def sendTinkoffSticker(id: Long): Unit = {
    bot(SendSticker(id.toString, tinkoffStickers(Random.nextInt(tinkoffStickers.length))))
  }

  private def sendCommandUnknown(cmd: String, msg: Message): Unit = {
    bot(msg.chat.sendMessage withText s"unknown command : $cmd")
  }

  private def sendRates(msg: Message): Unit = {
    val session = userMap(msg.chat.id).sessionId
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
    if (accounts.isEmpty) text.noOperations
    else {
      val operationsList = accounts.map(x => text.operationsInfo.format(
        millisToDate(x.operationTime.milliseconds), x.category.name, x.description, if (x.`type` == "Credit") "+" else "-", x.amount.value, x.amount.currency.name
      ))
      "<pre>" + operationsList.mkString("\n\n") + "</pre>"
    }
  }
}
