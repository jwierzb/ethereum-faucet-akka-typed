name := "SBTSampleExxample"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.0"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"

// Akka-http

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.19",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.akka" %% "akka-http" % "10.1.6",
  "com.typesafe.akka" %% "akka-stream" % "2.5.19",
  "com.typesafe.akka" %% "akka-actor" % "2.5.19",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.6",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.19" % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.6",
  "org.mockito" % "mockito-all" % "1.9.5" % Test,
  "com.typesafe.akka" %% "akka-actor-typed" % "2.5.19",
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.5.19" % Test,
  "org.scalatest" %% "scalatest" % "3.0.4" % Test


)
