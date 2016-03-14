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
import com.typesafe.config.ConfigFactory
// import scala.concurrent.ExecutionContext
// import scala.concurrent.ExecutionContext.Implicits.global
// import scala.concurrent.Future

class HttpServer(chatSystem: ActorRef, config: Config)(implicit actorSystem: ActorSystem) {
  import MarshallingHelpers._
  implicit val m = ActorMaterializer()

  val route: Route =
    path("room" / Segment) { room =>
      put {
        entity(as[String]) { msg =>
          chatSystem ! ChatSystem.Publish(room, msg)
          complete("ok")
        }
      } ~
      get {
        implicit val stringWriter = toPlainTextStream[String]
        complete {
          Source.actorRef[String](10, OverflowStrategy.dropTail).
            mapMaterializedValue { ref =>
              chatSystem ! ChatSystem.Subscribe(room, ref)
              ref
            }
        }
      }

    }
  
  def run() = {
    Http().bindAndHandle(
      Route.handlerFlow(route),
      config.getString("server.address"),
      config.getInt("server.port"))
  }
}

