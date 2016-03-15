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
import com.typesafe.scalalogging.LazyLogging

object Main extends App with LazyLogging {
  def bootLocalSystem(clusterPort: Int, httpPort: Int): Unit = {
    val config = ConfigFactory.parseString(s"""
    |  akka.remote.netty.tcp.port=${clusterPort}
    |  server.port=${httpPort}
    |""".stripMargin).
      withFallback(ConfigFactory.load())
    implicit val system = ActorSystem("ClusterSystem", config)
    val chatSystem = ChatSystem.start()
    val server = new HttpServer(chatSystem, config)
    server.run()
  }

  def bootConductrSystem(): Unit = {
    import com.typesafe.conductr.bundlelib.akka.Env
    import com.typesafe.config.ConfigFactory

    val config = Env.asConfig.withFallback(ConfigFactory.load())

    val systemName = (sys.env.get("BUNDLE_SYSTEM"), sys.env.get("BUNDLE_SYSTEM_VERSION")) match {
      case (Some(name), Some(version)) =>
        s"${name}-${version}"
      case _ =>
        "ChatClusterSystem"
    }

    logger.info("systemName: " + systemName)

    logger.info("AkkaClusterFrontend akka.remote.netty.tcp.hostname: " + config.getString("akka.remote.netty.tcp.hostname"))
    logger.info("AkkaClusterFrontend akka.remote.netty.tcp.port: " + config.getString("akka.remote.netty.tcp.port"))
    logger.info("AkkaClusterFrontend akka.cluster.seed-nodes: " + config.getList("akka.cluster.seed-nodes"))


    logger.info(s"AkkaClusterFrontend AKKA_REMOTE_HOST ${sys.env.get("AKKA_REMOTE_HOST")}")
    logger.info(s"Env asConfig ${Env.asConfig.toString}")

    logger.info(s"bundleHostIp ${sys.env.get("BUNDLE_HOST_IP")}")
    logger.info(s"bundleSystem ${sys.env.get("BUNDLE_SYSTEM")}")
    logger.info(s"akkaRemoteHostProtocol ${sys.env.get("AKKA_REMOTE_HOST_PROTOCOL")}")
    logger.info(s"akkaRemoteHostPort ${sys.env.get("AKKA_REMOTE_HOST_PORT")}")
    logger.info(s"akkaRemoteOtherProtocolsConcat ${sys.env.get("AKKA_REMOTE_OTHER_PROTOCOLS")}")
    logger.info(s"akkaRemoteOtherIpsConcat ${sys.env.get("AKKA_REMOTE_OTHER_IPS")}")
    logger.info(s"akkaRemoteOtherPortsConcat ${sys.env.get("AKKA_REMOTE_OTHER_PORTS")}")
    logger.info(s"SPRAY_HTTP_BIND_IP ${sys.env.get("CHAT_BIND_IP")}")
    logger.info(s"SPRAY_HTTP_BIND_PORT ${sys.env.get("CHAT_BIND_PORT")}")

    implicit val actorSystem = ActorSystem(systemName, config)
    // val config = 
    import com.typesafe.conductr.lib.akka.ConnectionContext
    import com.typesafe.conductr.bundlelib.akka.StatusService
    implicit val cc = ConnectionContext()
    val chatSystem = ChatSystem.start()
    val server = new HttpServer(chatSystem, config)
    server.run()
    StatusService.signalStartedOrExit()
  }

  if ((args.length > 0) && args(0) == "local") {
    bootLocalSystem(2551, 8080)
    bootLocalSystem(2552, 8081)
  } else {
    bootConductrSystem()
  }
}
