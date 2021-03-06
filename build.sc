import mill._
import mill.scalalib._
import mill.scalajslib._
import ammonite.ops._

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
  
  val csvName = "jmh-results.csv"
  val processedCsvName = "jmh-results-p.csv"
  
  def mkGraphs() = T.command {
    mkGraph(4)()
    mkGraph(16)()
    mkGraph(64)()
    //mkGraph(128)()
    mkGraph(256)()
  }
  
  // Processes the CSV generated by a previous run of `mkJmhCSV`, and makes a graph out of it
  def mkGraph(size: Int) = T.command {
    processCsv(size)()
    val processedCsvPath = T.ctx.dest/up/up/'processCsv/'dest/processedCsvName
    val wd = cwd/'benchmarks/'plots
    rm(wd/processedCsvName)
    cp(processedCsvPath, wd/processedCsvName)
    val output = s"out/jmh-results-$size.pdf"
    println(s"Generating plot in: $wd/$output")
    mkdir(wd/'out)
    %%("gnuplot", "-e", s"set output '$output'", s"mkplot.gnu")(wd = wd)
    rm(wd/processedCsvName)
  }
  
  def processCsv(size: Int) = T.command {
    val csvPath = T.ctx.dest/up/up/'runJmh/'dest/csvName
    
    val default = "0,0"
    
    def mk(val_err:Option[(Double,Double)]) =
      val_err.fold(default)(val_err => s"${val_err._1},${val_err._2}")
    
    val csvLines = scala.io.Source.fromFile(csvPath.toString).getLines()
    val headers = csvLines.next.split(',').zipWithIndex.toMap
    println("Headers: "+headers.keys)
    val groups = (for (line <- csvLines) yield {
      val segments = line.split(',')
      def get(headerName: String) = '"'+headerName+'"' |> headers |> segments
      val (name,score,err) = (get("Benchmark"),get("Score"),get("Score Error (99.9%)"))
      val shortName = name.init.tail.stripPrefix("benchmarks.").dropWhile(_ != '.').tail
      val funName = shortName.view.reverse.dropWhile(_ != '_').tail.reverse.force
      val kind = shortName.view.reverse.takeWhile(_ != '_').reverse.force
      val s = get("Param: size").toInt
      //println(shortName,funName,score)
      (funName, s, kind -> (score,err))
    }).toList.groupBy(_._1).mapValues(_.collect{case(_,`size`,v)=>v}.toMap)
    val lines = for ((name,gp) <- groups) yield {
      println(s"Entry: $name: $gp")
      //s"${name.replaceAll("_","-")},${mk(gp get "baseline")},${mk(gp get "for")},${mk(gp get "lazyfor")},${mk(gp get "lazyfused")}\n"
      val base = gp getOrElse ("for", sys.error(s"missing 'for' baseline for $name->$gp"))
      val baseScore = base._1.toDouble
      val baseErr = base._2.toDouble
      val res = gp.mapValues{case (scoreStr,errStr) =>
        val score = scoreStr.toDouble
        val scoreErr = errStr.toDouble
        val speedupRatio = score/baseScore
        val err = scoreErr/score * speedupRatio
        (speedupRatio,err)
      }
      List(name.replaceAll("_","-"),
        mk(res get "baseline"),
        mk(res get "for"),
        mk(res get "lazyfor"),
        mk(res get "lazyfused"),
        mk(res get "lazymixed"),
        mk(res get "lazyunboxed"),
        mk(res get "monadicfor"),
        mk(res get "liftedfor"),
      ).mkString("",",","\n")
    }
    scala.tools.nsc.io.File(T.ctx.dest/processedCsvName toString).writeAll(lines.toList:_*)
    
  }
  
  def mkJmhCSV(args: String*) = T.command {
    //val runJmhDest = runJmh("-rf" +: "csv" +: "-rff" +: csvName +: args : _*)()
    //runJmhDest/csvName
    // ^ this crashes due to a mill macro bug!!
    //   java.lang.IllegalArgumentException: Could not find proxy for val x$9: String in List(value x$9, value runJmhDest, method $anonfun$mkJmhCSV$2, method $anonfun$mkJmhCSV$1, method mkJmhCSV, class benchmarks, trait build, package $file, package ammonite, package <root>) (currentOwner= method mkJmhCSV )
    runJmh("-rf" +: "csv" +: "-rff" +: csvName +: args : _*)() |> { runJmhDest =>
      runJmhDest/csvName
    }
  }
  
}



// adapted from https://github.com/lihaoyi/mill/blob/master/integration/test/resources/play-json/jmh.sc

import ammonite.ops._
import mill._, scalalib._, modules._

trait Jmh extends ScalaModule {

  def ivyDeps = super.ivyDeps() ++ Agg(ivy"org.openjdk.jmh:jmh-core:1.19")

  def runJmh(args: String*) = T.command {
    val (_, resources) = generateBenchmarkSources()
    //println(s"Run JMH $args") // added
    Jvm.interactiveSubprocess(
      "org.openjdk.jmh.Main",
      classPath = (runClasspath() ++ generatorDeps()).map(_.path) ++
        Seq(compileGeneratedSources().path, resources),
      mainArgs = args,
      workingDir = T.ctx.dest
    )
    T.ctx.dest // added
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
