package model

import akka.actor._
import akka.util.duration._

import play.api.libs.iteratee._
import play.api.libs.concurrent._

import play.api.Play.current
import akka.util.Timeout
import akka.pattern._

object ChatRooms {

  implicit val timeout = Timeout(1 second)

  lazy val default = {
    val roomsActor = Akka.system.actorOf(Props[ChatRooms])

    roomsActor
  }

}

object Member {
  implicit val timeout = Timeout(1 second)

  def join(chatroomName:String):Promise[(Iteratee[Array[Byte],_],Enumerator[Array[Byte]])] = {
    (ChatRooms.default ? Get(chatroomName)).flatMap {
      case Chatroom(chatroom) => {
          val enumerator: PushEnumerator[Array[Byte]] = Enumerator.imperative[Array[Byte]]()
          val member = Akka.system.actorOf(Props(new Member(chatroom, enumerator)))
          (chatroom ? Join(member)).map {

          case Connected() =>
          // Create an Iteratee to consume the feed
          val iteratee = Iteratee.foreach[Array[Byte]] { message =>
          chatroom ! Talk( message)
        }.mapDone { _ =>
          chatroom ! Quit(member)
        }

          (iteratee,enumerator)

          case CannotConnect(error) =>
          // A finished Iteratee sending EOF
          val iteratee = Done[Array[Byte],Unit]((),Input.EOF)

          // Send an error and close the socket
          val enumerator =  Enumerator[Array[Byte]]().andThen(Enumerator.enumInput(Input.EOF))

          (iteratee,enumerator)

        }
      }
    }
  }.asPromise
}

class Member(chatRoom:ActorRef, outPut:PushEnumerator[Array[Byte]]) extends Actor {
    def receive = {
        case t:Talk => {
          outPut.push(t.image)
        }
    }
}

class ChatRooms extends Actor {

  var chatrooms = Map.empty[String, ActorRef]

  def receive = {
    case Get(chatroomName) => {
      // Create an Enumerator to write to this socket
      if(chatrooms.contains(chatroomName)) {
        sender ! Chatroom(chatrooms(chatroomName))
      } else {
        val chatroom: ActorRef = context.actorOf(Props(new ChatRoom(self, chatroomName)))
        chatrooms = chatrooms + (chatroomName -> chatroom)

        sender ! Chatroom(chatroom)
      }
    }
    case Empty(chatroomName) => {
      chatrooms = chatrooms - chatroomName
    }
  }

}

class ChatRoom(rooms:ActorRef, name:String) extends Actor {

  var members:List[ActorRef] = Nil

  def receive = {
    case Join(ref) => {
      // Create an Enumerator to write to this socket
      if(members.contains(ref)) {
        sender ! CannotConnect("already connected")
      } else {
        members = ref :: members

        sender ! Connected()
      }
    }

    case t:Talk => {
      notifyAll(t)
    }

    case Quit(ref) => {
      members = members.filterNot(_ == ref)
      if(members.size == 0){
        rooms ! Empty(name)
      }
    }

  }

  def notifyAll(message : Talk) {
    members.foreach {
      _ ! message
    }
  }

}

case class Join( ref: ActorRef)
case class Get(chatroomName: String)
case class Chatroom(chatroom: ActorRef)
case class Empty(name: String)
case class Quit(ref:ActorRef)
case class Talk(image: Array[Byte])
case class Connected()
case class CannotConnect(msg: String)