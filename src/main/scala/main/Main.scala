package main

import akka.actor.ActorIdentity
import akka.actor.ActorPath
import akka.pattern.ask
import akka.actor.ActorSystem
import akka.actor.Identify
import akka.actor.Props
import akka.persistence.journal.leveldb.SharedLeveldbJournal
import akka.persistence.journal.leveldb.SharedLeveldbStore
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

object Main extends App {
  def bootSystem(clusterPort: Int, httpPort: Int): Unit = {
    val config = ConfigFactory.parseString(s"""
    |  akka.remote.netty.tcp.port=${clusterPort}
    |  server.port=${httpPort}
    |""".stripMargin).
      withFallback(ConfigFactory.load())
    implicit val system = ActorSystem("ClusterSystem", config)
    startupSharedJournal(system, startStore = (clusterPort == 2551), path =
      ActorPath.fromString("akka.tcp://ClusterSystem@127.0.0.1:2551/user/store"))
    val chatSystem = ChatSystem.start()
    val server = new HttpServer(chatSystem, config)
    server.run()
  }

  bootSystem(2551, 8080)
  bootSystem(2552, 8081)


  def startupSharedJournal(system: ActorSystem, startStore: Boolean, path: ActorPath): Unit = {
    // Start the shared journal one one node (don't crash this SPOF)
    // This will not be needed with a distributed journal
    if (startStore)
      system.actorOf(Props[SharedLeveldbStore], "store")
    // register the shared journal
    import system.dispatcher
    implicit val timeout = Timeout(15.seconds)
    val f = (system.actorSelection(path) ? Identify(None))
    f.onSuccess {
      case ActorIdentity(_, Some(ref)) => SharedLeveldbJournal.setStore(ref, system)
      case _ =>
        system.log.error("Shared journal not started at {}", path)
        system.terminate()
    }
    f.onFailure {
      case _ =>
        system.log.error("Lookup of shared journal at {} timed out", path)
        system.terminate()
    }
  }

}
