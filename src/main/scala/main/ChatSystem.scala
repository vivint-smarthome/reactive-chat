package main

import akka.actor.ActorInitializationException
import akka.actor.ActorLogging
import akka.actor.ActorSystem
import akka.actor.DeathPactException
import akka.actor.OneForOneStrategy
import akka.actor.Props
import akka.actor.SupervisorStrategy
import akka.actor.Terminated
import akka.cluster.sharding.ClusterSharding
import akka.cluster.sharding.ClusterShardingSettings
import akka.cluster.sharding.ShardRegion
import scala.collection.mutable

import akka.actor.{ActorRef, Actor}

class ChatRoom extends Actor with ActorLogging {
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
    case msg =>
      log.info("=== I received a message {}", msg)

  }
}


class ChatSupervisor extends Actor with ActorLogging {
  val chatRoom = context.actorOf(Props[ChatRoom], "theRoom")
  
  log.info("hey there! {} is up", context.self.path)
  
  override val supervisorStrategy = OneForOneStrategy() {
    case _: IllegalArgumentException     => SupervisorStrategy.Resume
    case _: ActorInitializationException => SupervisorStrategy.Stop
    case _: DeathPactException           => SupervisorStrategy.Stop
    case _: Exception                    => SupervisorStrategy.Restart
  }
  
  def receive = {
    case msg => {
      chatRoom forward msg
    }

  }
}

object ChatSystem {
  case class Subscribe(room: String, ref: ActorRef)
  case class Publish(room: String, text: String)

  def extractEntityId: ShardRegion.ExtractEntityId = {
    case s@ChatSystem.Subscribe(room, ref) =>
      (room, s)
    case p@ChatSystem.Publish(room, msg) =>
      (room, p)
  }

  def extractShardId: ShardRegion.ExtractShardId = {
    case ChatSystem.Subscribe(room, _) =>
      room
    case ChatSystem.Publish(room, _) =>
      room
  }

  def start()(implicit system: ActorSystem): ActorRef = {
    ClusterSharding(system).start(
      typeName = "SupervisedChatRoom",
      entityProps = Props[ChatSupervisor],
      settings = ClusterShardingSettings(system),
      extractEntityId = extractEntityId,
      extractShardId = extractShardId)
  }

}

