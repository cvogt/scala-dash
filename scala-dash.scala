import ammonite.ops._
import play.api.libs.json._
import java.net.URL
import Lib._


class SlickDocset(version: String) extends Docset(
  s"slick.typesafe.com-$version",
  Seq("-*api/*","-*-api/*")
){
  override def url = s"http://slick.typesafe.com/doc/$version/" 
  override def icon32x32 = "slick.typesafe.com.png"
  override def feed = "slick.typesafe.com"
}

object ScalaDashDocsetBuilder{
  val docsets = Vector(
    new SlickDocset("3.1.0-M2"),
    new SlickDocset("3.0.2"),
    new SlickDocset("2.1.0"),
    new Docset(
      "docs.scala-lang.org",
      Seq("-*/de/*","-*/es/*","-*/fr/*","-*/ja/*","-*/ko/*","-*/pt-br/*","-*/zh-cn/*")
    ),
    new Docset(
      "Ammonite"
    ){
      override def url = "http://lihaoyi.github.io/Ammonite/"
      override def index = "Ammonite/index.html"
      override def selectors: Map[String,String] = Map(
        "h1" -> "Section"
      )
      override def ignore = Vector(
        "0.4.6", "0.4.5", "0.4.4", "0.4.3", "0.4.2", "0.4.1", "0.4.0", "0.3.2", "0.3.1", "0.3.0", "0.2.9", "0.2.8", "404"
      )
  },
    new Docset(
      "learning-scalaz",
      Vector("-*/ja/*","-*/7.0/*")
    ){
      override def url = "http://eed3si9n.com/learning-scalaz/Combined+Pages.html"
      override def name = "learning Scalaz"
      override def index = "learning-scalaz/Combined+Pages.html"
      override def ignore = Vector("Page not found | eed3si9n")
      override def selectors: Map[String,String] = Map(
        "h1" -> "Section",
        "h2" -> "Section",
        "h3" -> "Section"
      )
      override def download: Unit = {
        val _url = new URL(url)
        rm( cwd/'temp/docsetName )
        mkdir( cwd/'temp/docsetName )
        %(cwd/'temp/docsetName)wget(
          "--convert-links",
          "--page-requisites",
          "-e", "robots=off",
          url
        )
        rm( cwd/'html/docsetName )
        mv( cwd/'temp/docsetName/_url.getHost, cwd/'html/docsetName )
        rm( cwd/'temp/docsetName )
      }
    }
  )

  def main(args: Array[String]): Unit = {
    args match{
      case Array("all") =>
        docsets.foreach{ d =>
          d.download
          d.build
        }
      case Array("download", "all") => docsets.foreach(_.download)
      case Array("download", docset) => docsets.find(_.docsetName == docset).map(_.download).getOrElse(usage)
      case Array("build", "all") => docsets.foreach(_.build)
      case Array("build", docset) => docsets.find(_.docsetName == docset).map(_.build).getOrElse(usage)
      case Array("feed") =>
        val feedXml = docsets.lastOption.toSeq.map( d =>
<entry>
  <version>{java.time.LocalDateTime.now.toString}</version>
  <url>http://cvogt.org/releases/docsets/{d.docsetName}.tgz</url>
  <other-versions>
    <version><name>2.1.48</name></version>
    <version><name>2.1.47</name></version>
    <version><name>2.1.46</name></version>
  </other-versions>
</entry>
        ).map(_.toString).mkString("\n")
        val feedFile = cwd/'archives/"learning-scalaz.xml"
        rm(feedFile)
        write(feedFile, feedXml)
      case _ => usage
    }
  }

  def usage = println(s"""
USAGE:
sbt run <command> [<docset>]

supported <command>s

  download - downloads the given docset
  build    - creates docset
  all      - download and build all docsets

supported <docset>s

  all - all docsets
  or pick one: ${docsets.map(_.docsetName).mkString(", ")}
""")
}

class Docset(
  val docsetName: String,
  httrackArguments: Seq[Shellable] = Seq()
){
  def name = docsetName
  def feed = name

  def `package` = "scala"
  def icon32x32 = docsetName ++ ".png"
  def index: String = "index.html"
  def selectors: Map[String,String] = Map(
    "dt a" -> "Command",
    "title" -> "Section" // Category, Entry, Command, Element, 
  )
  def ignore: Vector[String] = Vector("ABOUT")
  def allowJs: Boolean = true
  def url = s"http://$docsetName/"

  final def build = {
    val iconTarget = cwd/'html/docsetName/icon32x32
    try{
      val iconSource = cwd/'logos/icon32x32
      stat! iconSource // tests weather exists
      rm( iconTarget )
      cp( iconSource, iconTarget )
    } catch {
      case _:java.nio.file.NoSuchFileException => println("[warning] no icon found for "+docsetName)
    }
      //%jekyll "build"
    val configTarget = cwd/'html/docsetName/"dashing.json"
    rm( configTarget )
    write( configTarget, json )
    %(cwd/'html/docsetName) dashing 'build
    val docsetFile = cwd/'docsets/(docsetName++".docset")
    rm( docsetFile ) 
    mv( cwd/'html/docsetName/(`package`++".docset"), docsetFile ) 
    rm( configTarget )
    rm( iconTarget )
    archive
  }

  def archive = {
    val tarTarget = cwd/'archives/(docsetName++".tgz")
    rm(tarTarget)
    %(cwd) tar(
      "--exclude='.DS_Store'",
      "-cvzf",
      tarTarget,
      cwd/'docsets/(docsetName++".docset")
    )
  }
  def json = Json.prettyPrint(
    Json.obj(
      "name" -> JsString(name),
      "package" -> JsString(`package`),
      "index" -> JsString(index),
      "selectors" -> JsObject(selectors.mapValues(JsString(_))),
      "ignore" -> JsArray(ignore.map(JsString(_))),
      "icon32x32" -> JsString(icon32x32),
      "allowJs" -> JsBoolean(allowJs)
    )
  )

  def setup = ()
  def cleanup = ()
  def download: Unit = {
    val _url = new URL(url)
    rm( cwd/'temp/docsetName )
    mkdir( cwd/'temp/docsetName )
    mirror( cwd/'temp )
    rm( cwd/'html/docsetName )
    mv( cwd/'temp/docsetName/_url.getHost, cwd/'html/docsetName )
    rm( cwd/'temp/docsetName )
  }
  private def mirror(cwd: Path): Unit = {
    val args = Seq[Shellable]( "--mirror", url ) ++ httrackArguments
    %(cwd).applyDynamic("httrack")(args: _*)
  }
}


object Lib{
  /*wget(
    "--continue",
    "--mirror",
    "--html-extension",
    "--convert-links",
    "--page-requisites",
    "----no-parent",
    url
  )*/
}
