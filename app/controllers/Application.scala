package controllers

import play.api._
import libs.iteratee.{PushEnumerator, Enumerator, Iteratee}
import play.api.mvc._
import scala.Predef._
import model.{ChatRoom, Member}

object Application extends Controller {

  def index = WebSocket.using[Array[Byte]] { request =>
    val out = Enumerator.imperative[Array[Byte]]()
    val in = Iteratee.foreach[Array[Byte]](doThing(out)).mapDone(_ => println("Disconnected"))
    (in, out)
  }

  def chat(username: String) = WebSocket.async[Array[Byte]] { request =>
    Member.join(username, ChatRoom.default)
  }


  def doThing(out: PushEnumerator[Array[Byte]]): (Array[Byte]) => Unit = {
    x => println(x);out.push(x)
  }
}