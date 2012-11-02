package controllers

import play.api._
import libs.iteratee.{PushEnumerator, Enumerator, Iteratee}
import play.api.mvc._
import scala.Predef._
import model.{ChatRoom, Member}

object Application extends Controller {

  def default() = Action { request =>
    Ok(views.html.index("default"))
  }

  def index(chatroom: String) = Action { request =>
    Ok(views.html.index(chatroom))
  }

  def chat(chatroom: String) = WebSocket.async[Array[Byte]] { request =>
    println("hi")
    Member.join(chatroom)
  }


  def doThing(out: PushEnumerator[Array[Byte]]): (Array[Byte]) => Unit = {
    x => println(x);out.push(x)
  }
}