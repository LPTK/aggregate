import mill._
import mill.scalalib._
import mill.scalajslib._

val scala212Version = "2.12.6"
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

object benchmarks extends Cross[benchmarks](scala211Version, scala212Version)
class benchmarks(val crossScalaVersion: String) extends CrossSbtModule with Jmh {
   
}



// copied from https://github.com/lihaoyi/mill/blob/master/integration/test/resources/play-json/jmh.sc

import ammonite.ops._
import mill._, scalalib._, modules._

trait Jmh extends ScalaModule {

  def ivyDeps = super.ivyDeps() ++ Agg(ivy"org.openjdk.jmh:jmh-core:1.19")

  def runJmh(args: String*) = T.command {
    val (_, resources) = generateBenchmarkSources()
    Jvm.interactiveSubprocess(
      "org.openjdk.jmh.Main",
      classPath = (runClasspath() ++ generatorDeps()).map(_.path) ++
        Seq(compileGeneratedSources().path, resources),
      mainArgs = args,
      workingDir = T.ctx.dest
    )
  }

  def compileGeneratedSources = T {
    val dest = T.ctx.dest
    val (sourcesDir, _) = generateBenchmarkSources()
    val sources = ls.rec(sourcesDir).filter(_.isFile)
    %%("javac",
       sources.map(_.toString),
       "-cp",
       (runClasspath() ++ generatorDeps()).map(_.path.toString).mkString(":"),
       "-d",
       dest)(wd = dest)
    PathRef(dest)
  }

  // returns sources and resources directories
  def generateBenchmarkSources = T {
    val dest = T.ctx().dest

    val sourcesDir = dest / 'jmh_sources
    val resourcesDir = dest / 'jmh_resources

    rm(sourcesDir)
    mkdir(sourcesDir)
    rm(resourcesDir)
    mkdir(resourcesDir)

    Jvm.subprocess(
      "org.openjdk.jmh.generators.bytecode.JmhBytecodeGenerator",
      (runClasspath() ++ generatorDeps()).map(_.path),
      mainArgs = Array(
        compile().classes.path,
        sourcesDir,
        resourcesDir,
        "default"
      ).map(_.toString)
    )

    (sourcesDir, resourcesDir)
  }

  def generatorDeps = resolveDeps(
    T { Agg(ivy"org.openjdk.jmh:jmh-generator-bytecode:1.19") }
  )
}
