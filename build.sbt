name := "Reactive Chat"

scalaVersion := "2.11.8"

val akkaVersion = "2.4.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
  "com.typesafe.play" %% "play-json" % "2.4.3",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion,
  "com.typesafe.akka" %% "akka-distributed-data-experimental" % akkaVersion,
  // "org.iq80.leveldb" % "leveldb" % "0.7",

  "com.typesafe.scala-logging" %% "scala-logging" % "3.0.0",
  "com.typesafe.conductr" %% "akka24-conductr-bundle-lib" % "1.4.0",
  "ch.qos.logback" % "logback-classic" % "1.1.2"
)

resolvers += "typesafe-releases" at "http://repo.typesafe.com/typesafe/maven-releases"

SandboxKeys.imageVersion in Global := "1.1.2"

enablePlugins(JavaAppPackaging)

// logLevel in Global := Level.Debug

import ByteConversions._
BundleKeys.nrOfCpus := 1.0
BundleKeys.memory := 128.MiB
BundleKeys.diskSpace := 10.MB
BundleKeys.roles := Set("chat")
BundleKeys.endpoints := Map(
  "akka-remote" -> Endpoint("tcp"),
  "chat" -> Endpoint("http", services = Set(URI("http://:9000"))))
