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

  def join(chatroomName:String):Promise[(Iteratee[Either[String,Array[Byte]],_],Enumerator[Either[String,Array[Byte]]])] = {
    (ChatRooms.default ? Get(chatroomName)).flatMap {
      case Chatroom(chatroom) => {
          val enumerator: PushEnumerator[Either[String,Array[Byte]]] = Enumerator.imperative[Either[String,Array[Byte]]]()
          val member = Akka.system.actorOf(Props(new Member(chatroom, enumerator)))
          (chatroom ? Join(member)).map {

          case Connected() =>
          // Create an Iteratee to consume the feed
          val iteratee = Iteratee.foreach[Either[String,Array[Byte]]] {
            case Right(bytes)  =>
              chatroom ! Talk( bytes)
            case Left(string) =>
              chatroom ! Jabber( string)
        }.mapDone { _ =>
          chatroom ! Quit(member)
        }

          (iteratee,enumerator)

          case CannotConnect(error) =>
          // A finished Iteratee sending EOF
          val iteratee = Done[Either[String,Array[Byte]],Unit]((),Input.EOF)

          // Send an error and close the socket
          val enumerator =  Enumerator[Either[String,Array[Byte]]]().andThen(Enumerator.enumInput(Input.EOF))

          (iteratee,enumerator)

        }
      }
    }
  }.asPromise
}

class Member(chatRoom:ActorRef, outPut:PushEnumerator[Either[String,Array[Byte]]]) extends Actor {
    def receive = {
        case t:Talk => {
          outPut.push(Right(t.image))
        }
        case t:Jabber => {
          outPut.push(Left(t.words))
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
        updateAllOnlineMembers
      }
    }

    case t:Talk => {
      notifyAll(t)
    }

    case Jabber("$DRAWSHIT$online$") => {
      updateAllOnlineMembers
    }

    case t:Jabber => {
      notifyAll(t)
    }

    case Quit(ref) => {
      members = members.filterNot(_ == ref)
      if(members.size == 0){
        rooms ! Empty(name)
      }
    }

  }


  def updateAllOnlineMembers {
    notifyAll(Jabber("$DRAWSHIT$online$" + members.length))
  }

  def notifyAll(message : Message) {
    members.foreach {
      _ ! message
    }
  }

}

sealed abstract class Message
case class Join( ref: ActorRef)           extends Message
case class Get(chatroomName: String)    extends Message
case class Chatroom(chatroom: ActorRef)  extends Message
case class Empty(name: String)           extends Message
case class Quit(ref:ActorRef)            extends Message
case class Talk(image: Array[Byte])     extends Message
case class Jabber(words: String)          extends Message
case class Connected()                   extends Message
case class CannotConnect(msg: String)     extends Message