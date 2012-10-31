package model

import akka.actor._
import akka.util.duration._

import play.api.libs.iteratee._
import play.api.libs.concurrent._

import play.api.Play.current
import akka.util.Timeout
import akka.pattern._

object ChatRoom {

  implicit val timeout = Timeout(1 second)

  lazy val default = {
    val roomActor = Akka.system.actorOf(Props[ChatRoom])

    roomActor
  }

}

object Member {
  implicit val timeout = Timeout(1 second)

  def join(username:String, chatroom : ActorRef):Promise[(Iteratee[Array[Byte],_],Enumerator[Array[Byte]])] = {
    val enumerator: PushEnumerator[Array[Byte]] = Enumerator.imperative[Array[Byte]]()
    val member = Akka.system.actorOf(Props(new Member(chatroom, enumerator)))
    println("shit")
    (chatroom ? Join(username,member)).map {

      case Connected() =>
        // Create an Iteratee to consume the feed
        val iteratee = Iteratee.foreach[Array[Byte]] { message =>
          chatroom ! Talk(username, message)
        }.mapDone { _ =>
          chatroom ! Quit(username)
        }

        (iteratee,enumerator)

      case CannotConnect(error) =>
        // A finished Iteratee sending EOF
        val iteratee = Done[Array[Byte],Unit]((),Input.EOF)

        // Send an error and close the socket
        val enumerator =  Enumerator[Array[Byte]]().andThen(Enumerator.enumInput(Input.EOF))

        (iteratee,enumerator)

    }.asPromise
  }
}

class Member(chatRoom:ActorRef, outPut:PushEnumerator[Array[Byte]]) extends Actor {
    def receive = {
        case t:Talk => {
          println("shit")
          outPut.push(t.image)
        }
    }
}

class ChatRoom extends Actor {

  var members = Map.empty[String, ActorRef]

  def receive = {
    case Join(username,ref) => {
      // Create an Enumerator to write to this socket
      if(members.contains(username)) {
        sender ! CannotConnect("This username is already used")
      } else {
        members = members + (username -> ref)

        sender ! Connected()
      }
    }

    case t:Talk => {
      notifyAll(t)
    }

    case Quit(username) => {
      members = members - username
    }

  }

  def notifyAll(message : Talk) {
    members.foreach {
      case (_, ref) => println("wootzer"); ref ! message
    }
  }

}

case class Join(username: String, ref: ActorRef)
case class Quit(username: String)
case class Talk(username: String, image: Array[Byte])
case class Connected()
case class CannotConnect(msg: String)