enablePlugins(JavaAppPackaging)

name := """opentab"""
organization := "eu.seitzal"

version := "0.2-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

.settings(PlayKeys.playDefaultPort := 8080)

scalaVersion := "2.13.1"

scalacOptions += "-target:jvm-1.8"
javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

// Backend dependencies
libraryDependencies ++= Seq(
  guice,
  jdbc,
  "org.mariadb.jdbc" %  "mariadb-java-client" % "2.4.3",
  "de.svenkubiak"    %  "jBCrypt"             % "0.4",
  "com.lihaoyi"      %% "upickle"             % "0.9.5")

// Frontend dependencies using WebJars
libraryDependencies ++= Seq (
  "org.webjars.npm" % "vue" % "2.6.11",
  "org.webjars.npm" % "vuex" % "3.1.2",
  "org.webjars.npm" % "vue-router" % "3.1.3",
  "org.webjars.npm" % "http-vue-loader" % "1.4.1",
  "org.webjars.npm" % "vuetify" % "2.2.14",
  "org.webjars.npm" % "jquery" % "3.4.1",
  "org.webjars.npm" % "js-cookie" % "2.2.1")
