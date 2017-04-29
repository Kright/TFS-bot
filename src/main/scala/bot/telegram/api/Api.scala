package bot.telegram.api

/**
  * Created by lgor on 4/29/17.
  */
case class User(id: Long, first_name: String, last_name: Option[String], username: Option[String])

case class MessageEntity(`type`: String, offset: Int, length: Int, url: Option[String])

case class Update(update_id: Int, message: Option[Message])

case class Message(message_id: Int,
                   from: Option[User],
                   date: Int,
                   chat: Chat,
                   text: Option[String],
                   entities: Option[List[MessageEntity]]) {

  def entity(typeName: String): Option[String] = {
    for (txt <- text;
         entity <- entities.flatMap(_.find(_.`type` == typeName)))
      return Option(txt.substring(entity.offset, entity.offset + entity.length))

    None
  }
}

case class Chat(id: Int, `type`: String,
                username: Option[String],
                first_name: Option[String],
                last_name: Option[String])


case class Response(ok: Boolean, description: Option[String], result: Option[List[Update]])
