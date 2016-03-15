package main

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.OverflowStrategy
import com.typesafe.config.Config
// import akka.http.scaladsl.model.headers
// import akka.http.scaladsl.model.headers.GenericHttpCredentials
// import akka.http.scaladsl.model.{StatusCodes, Uri}
import akka.stream.scaladsl._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
// import akka.stream.scaladsl.Source
// import com.typesafe.config.ConfigFactory
// import scala.concurrent.ExecutionContext
// import scala.concurrent.ExecutionContext.Implicits.global
// import scala.concurrent.Future

class HttpServer(chatSystem: ActorRef, config: Config)(implicit actorSystem: ActorSystem) {
  import MarshallingHelpers._
  implicit val m = ActorMaterializer()
  implicit val stringWriter = toPlainTextStream[String]

  val route: Route =
    path("room" / Segment) { room =>
      put {
        entity(as[String]) { msg =>
          chatSystem ! ChatSystem.Publish(room, msg)
          complete("ok")
        }
      } ~
      get {
        parameter('qty.as[Int].?) { qty =>
          val messages = 
            Source.actorRef[String](10, OverflowStrategy.dropTail).
              mapMaterializedValue { ref =>
                chatSystem ! ChatSystem.Subscribe(room, ref)
                ref
              }
          complete {
            qty match {
              case Some(q) =>
                messages.take(q)
              case _ =>
                messages
            }
          }
        }
      }
    }
  
  def run() = {
    Http().bindAndHandle(
      route,
      config.getString("server.address"),
      config.getInt("server.port"))
  }
}

