package controllers

import java.sql.SQLException
import java.util.Date
import javax.inject._
import play.api.mvc._
import play.api.db.Database
import play.api.libs.json.Json
import anorm._

@Singleton
class PostController @Inject()(db: Database, cc: ControllerComponents) extends AbstractController(cc) {

  case class postsData(id: String, user_id: String, text: String, posted_at: Date)
  case class commentsData(id: String, user_id: String, text: String, parent_post_id: String, posted_at: Date)


  val postsParser = {
    SqlParser.str("id") ~
      SqlParser.str("user_id") ~
      SqlParser.str("text") ~
      SqlParser.date("posted_at")
  } map {
    case id ~ user_id ~ text ~ posted_at =>
      postsData(id, user_id, text, posted_at)
  }

  val commentsParser = {
    SqlParser.str("id") ~
      SqlParser.str("user_id") ~
      SqlParser.str("text") ~
      SqlParser.str("parent_post_id") ~
      SqlParser.date("posted_at")
  } map {
    case id ~ user_id ~ text ~ parent_post_id ~ posted_at =>
      commentsData(id, user_id, text, parent_post_id,  posted_at)
  }


  def index() = Action { implicit request =>
    var user_id = ""
    var tmp_map = Map[String, String]()
    var result = Seq(Map[String, String]())
    var count:Int = 0
    db.withConnection { implicit conn =>
      val records1 = SQL("SELECT id, user_id, text, posted_at FROM posts").as(postsParser.*)
      val records2 = SQL("SELECT * FROM comments").as(commentsParser.*)
      val records = records1 +: records2
      println(records)
//      val records = SQL("SELECT * FROM posts").as(postsParser.*)
      for (s <- records) {
        println(s)
//        tmp_map += ("id" -> s.id, "user_id" -> s.user_id, "text" -> s.text, "posted_at" -> s.posted_at.toString)
        if (count == 0) {
          result = Seq(tmp_map)
        } else {
          result = result :+ tmp_map
        }
        count += 1
      }
      Ok(Json.toJson("posts" -> result))
    }
  }

  def create() = Action { request =>
    val form: Option[Map[String, Seq[String]]] = request.body.asFormUrlEncoded
    val param: Map[String, Seq[String]] = form.getOrElse(Map())
    val user_id: String = param.get("user_id").get(0)
    val text: String = param.get("text").get(0)

    var msg = "database"
    try {
      db.withConnection { conn =>
        val ps = conn.prepareStatement("insert into posts (id, user_id, text) values (?, ?, ?)")
        ps.setString(1, uuid)
        ps.setString(2, user_id)
        ps.setString(3, text)
        ps.executeUpdate
      }
    } catch {
      case e: SQLException =>
        msg += e
    }
    Ok(Json.toJson(Map("result" -> "OK")))
  }

  def uuid = java.util.UUID.randomUUID.toString

}
