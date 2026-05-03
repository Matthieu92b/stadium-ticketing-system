ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.7"

val akkaVersion      = "2.8.8"
val scalatestVersion = "3.2.19"

lazy val root = (project in file("."))
  .settings(
    name := "stadium-ticketing-system",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
      "com.typesafe.akka" %% "akka-slf4j"               % akkaVersion,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,

      "org.scalatest" %% "scalatest" % scalatestVersion % Test,

      "ch.qos.logback" % "logback-classic" % "1.2.13"
    )
  )