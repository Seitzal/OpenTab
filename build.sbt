enablePlugins(JavaAppPackaging)

name := """opentab"""
organization := "eu.seitzal"

version := "0.1-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

.settings(PlayKeys.playDefaultPort := 8080)

scalaVersion := "2.13.0"

scalacOptions += "-target:jvm-1.8"
javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

libraryDependencies += guice
libraryDependencies += jdbc
libraryDependencies += "org.mariadb.jdbc" % "mariadb-java-client" % "2.4.3"
libraryDependencies += "de.svenkubiak" % "jBCrypt" % "0.4"
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.example.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"
