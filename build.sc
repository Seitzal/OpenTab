import mill._, scalalib._

object server extends ScalaModule {
  def scalaVersion = "2.13.2"
  def ivyDeps = Agg(
    ivy"com.typesafe:config:1.4.0",
    ivy"ch.qos.logback:logback-classic:1.2.3",
    ivy"com.typesafe.scala-logging::scala-logging:3.9.2",
    ivy"org.http4s::http4s-dsl:0.21.3",
    ivy"org.http4s::http4s-blaze-server:0.21.3",
    ivy"com.lihaoyi::upickle:1.1.0",
    ivy"eu.seitzal::http4s-upickle:0.2.0",
    ivy"org.tpolecat::doobie-core:0.8.8",
    ivy"org.tpolecat::doobie-postgres:0.8.8"
  )
  def scalacOptions = Seq(
    "-deprecation"
  )
}