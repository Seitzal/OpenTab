enablePlugins(JavaAppPackaging)

name := """opentab"""
organization := "eu.seitzal"

version := "0.1-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

.settings(PlayKeys.playDefaultPort := 8080)

scalaVersion := "2.13.0"

scalacOptions += "-target:jvm-1.8"
javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

// Backend dependencies
libraryDependencies ++= Seq(
  guice,
  jdbc,
  "org.mariadb.jdbc" % "mariadb-java-client" % "2.4.3",
  "de.svenkubiak" % "jBCrypt" % "0.4",
  "com.lihaoyi" %% "upickle" % "0.7.5")

// Frontend dependencies using WebJars
libraryDependencies ++= Seq (
  "org.webjars.npm" % "vue" % "2.6.11",
  "org.webjars.npm" % "vuetify" % "2.2.14",
  "org.webjars.npm" % "axios" % "0.19.2")
