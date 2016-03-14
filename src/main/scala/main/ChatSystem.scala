package main

import akka.actor.ActorLogging
import akka.actor.Props
import akka.actor.Terminated
import scala.collection.mutable

import akka.actor.{ActorRef, Actor}

object ChatSystem {
  case class Subscribe(room: String, ref: ActorRef)
  case class Publish(room: String, text: String)
}

class ChatRoom(room: String) extends Actor with ActorLogging {
  import ChatSystem._

  private val subscribers = mutable.Set.empty[ActorRef]

  override def receive = {
    case Terminated(ref) =>
      log.info("subscriber {} dropped", ref)
      subscribers -= ref
    case s @ Subscribe(_, ref) =>
      log.info("subscribe {} joined", ref)
      if (!subscribers.contains(ref)) {
        subscribers += ref
        context.watch(ref)
      }
    case p @ Publish(_, msg) =>
      log.info("publishing msg {}", msg)
      subscribers.foreach(_ ! msg)
  }
}


class ChatSystem extends Actor {
  import ChatSystem._
  private val rooms = mutable.HashMap.empty[String, ActorRef]
  override def receive = {
    case s @ Subscribe(room, _) =>
      createIfNotExist(room) ! s
    case p @ Publish(room, msg) =>
      createIfNotExist(room) ! p
  }

  def createIfNotExist(room: String): ActorRef = {
    if (rooms.contains(room))
      return rooms(room)
    else {
      val newRoom = context.actorOf(Props { new ChatRoom(room) }, room)
      rooms(room) = newRoom
      newRoom
    }
  }
}

