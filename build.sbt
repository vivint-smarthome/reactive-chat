name := "Reactive Chat"

scalaVersion := "2.11.8"

val akkaVersion = "2.4.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
  "com.typesafe.play" %% "play-json" % "2.4.3",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion,
  "org.iq80.leveldb" % "leveldb" % "0.7"
)
