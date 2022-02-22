import mill._, scalalib._

object server extends ScalaModule {
  def scalaVersion = "2.13.8"
  def ivyDeps = Agg(
    ivy"com.typesafe:config:1.4.2",
    ivy"ch.qos.logback:logback-classic:1.2.10",
    ivy"com.typesafe.scala-logging::scala-logging:3.9.4",
    ivy"org.http4s::http4s-dsl:0.23.10",
    ivy"org.http4s::http4s-blaze-server:0.23.10",
    ivy"org.tpolecat::doobie-core:1.0.0-RC2",
    ivy"org.tpolecat::doobie-postgres:1.0.0-RC2",
    ivy"org.tpolecat::doobie-hikari:1.0.0-RC2",
    ivy"com.lihaoyi::upickle:1.5.0",
    ivy"de.svenkubiak:jBCrypt:0.4.3",
    ivy"com.pauldijou::jwt-upickle:5.0.0"
  )
  def scalacOptions = Seq(
    "-deprecation"
  )
}
