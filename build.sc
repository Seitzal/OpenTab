import mill._, scalalib._

object server extends ScalaModule {
  def scalaVersion = "2.13.2"
  def ivyDeps = Agg(
    ivy"eu.seitzal::gensrv-core:0.1.2",
    ivy"de.svenkubiak:jBCrypt:0.4",
    ivy"com.pauldijou::jwt-upickle:4.2.0"
  )
  def scalacOptions = Seq(
    "-deprecation"
  )
}