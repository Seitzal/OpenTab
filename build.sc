import mill._, scalalib._

object server extends ScalaModule {
  def scalaVersion = "2.13.2"
  def ivyDeps = Agg(
    ivy"eu.seitzal::gensrv-core:0.1.0",
    ivy"de.svenkubiak:jBCrypt:0.4",
  )
  def scalacOptions = Seq(
    "-deprecation"
  )
}