ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.7"

val akkaVersion = "2.6.21"

lazy val root = (project in file("."))
  .settings(
    name := "stadium-ticketing-system",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion cross CrossVersion.for3Use2_13,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion cross CrossVersion.for3Use2_13,
      "com.typesafe.akka" %% "akka-slf4j"               % akkaVersion cross CrossVersion.for3Use2_13,
      "ch.qos.logback"    %  "logback-classic"          % "1.2.13",
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test cross CrossVersion.for3Use2_13,
      "org.scalatest"     %% "scalatest"                % "3.2.17"    % Test
    )
  )