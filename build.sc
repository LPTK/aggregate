import mill._
import mill.scalalib._
import mill.scalajslib._

val scala212Version = "2.12.4"
val scala211Version = "2.11.11"

object aggregate extends Cross[aggregate](scala211Version, scala212Version)
class aggregate(val crossScalaVersion: String) extends CrossSbtModule {
   object test extends Tests {
      def testFrameworks = Seq("utest.runner.Framework")
   }
   def ivyDeps = Agg(
      ivy"com.lihaoyi::utest:0.6.0"
   )
   def scalacOptions = Seq("-deprecation", "-feature")
}
object aggregateJs extends Cross[aggregate](scala211Version, scala212Version)
class aggregateJs(crossScalaVersion: String) extends aggregate(crossScalaVersion) with ScalaJSModule {
   def scalaJSVersion = "0.6.22"
}
