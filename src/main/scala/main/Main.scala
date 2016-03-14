package main

import akka.actor.ActorSystem
import akka.actor.Props


object Main extends App {
  implicit val as = ActorSystem("le-system")
  val chatSystem = as.actorOf(Props[ChatSystem], "chat-system")
  val server = new HttpServer(chatSystem)
  server.run()
}
