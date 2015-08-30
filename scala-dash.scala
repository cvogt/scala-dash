import ammonite.ops._
import play.api.libs.json._
import java.net.URL
import Lib._

object ScalaDashDocsetBuilder{
  val docsets = Vector(
    new VersionedDocset(
      "slick.typesafe.com",
      Vector("-*api/*","-*-api/*"),
      Vector("3.0.2","3.1.0-M2","2.1.0"),
      version => s"http://slick.typesafe.com/doc/$version/" 
    ),
    new Docset(
      "docs.scala-lang.org",
      Vector("-*/de/*","-*/es/*","-*/fr/*","-*/ja/*","-*/ko/*","-*/pt-br/*","-*/zh-cn/*")
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
          d.feed
        }
      case Array("feed", "all") => docsets.foreach(_.feed)
      case Array("download", "all") => docsets.foreach(_.download)
      case Array("build", "all") => docsets.foreach(_.build)
      case Array("feed", docset) => docsets.find(_.docsetName == docset).map(_.feed).getOrElse(usage)
      case Array("download", docset) => docsets.find(_.docsetName == docset).map(_.download).getOrElse(usage)
      case Array("download", docset, version) => docsets.collect{
        case d: VersionedDocset if d.docsetName == docset => d
      }.headOption.map(_.docsets(version).download).getOrElse(usage)
      case Array("build", docset) => docsets.find(_.docsetName == docset).map(_.download).getOrElse(usage)
      case Array("build", docset, version) => docsets.collect{
        case d: VersionedDocset if d.docsetName == docset => d
      }.headOption.map(_.docsets(version).build).getOrElse(usage)
    }
  }

  def usage = println(s"""
USAGE:
sbt run <command> [<docset> [<version>]] 

supported <command>s

  download - downloads the given docset
  build    - creates docset
  all      - download and build all docsets

supported <docset>s and versions

  all - all docsets
  ${docsets.map{
    d => d.docsetName ++ ( d match {
      case v: VersionedDocset => v.versions.mkString("[",", ","]")
      case _ => ""
    })
  }.mkString("\n  ")}
""")
}
trait AbstractDocset{
  def docsetName: String
  def feed: Unit
  def build: Unit
  def download: Unit
}

class VersionedDocset(
  val docsetName: String,
  httrackArguments: Vector[Shellable],
  val versions: Vector[String],
  versionedUrl: String => String
) extends AbstractDocset{
  def docsets = versions.map(
    version =>
      version -> new Docset(
        docsetName,
        httrackArguments
      ){
        override def url = versionedUrl(version)
      }
  ).toMap
  def download = docsets.values.map(_.download)
  def build = {
    // reverse, so first version remains as most recent
    versions.reverse.map{ case version =>
      docsets(version).build
      val dir = cwd/'archives/'versions/docsetName/version
      mkdir(dir)
      val target = dir/(docsetName+".tgz")
      rm! target
      cp(cwd/'archives/(docsetName+".tgz"), target)
    }
  }
  def feed = {    
    val feedXml = 
<entry>
  <version>{versions.head}</version>
  <url>http://cvogt.org/releases/docsets/{docsetName}.tgz</url>
  <other-versions>{
    versions.map( v =>
      <version><name>{v}</name></version>
    )
  }</other-versions>
</entry>

      val feedFile = cwd/'archives/(docsetName++".xml")
      rm(feedFile)
      write(feedFile, feedXml.toString)
  }
}

class Docset(
  val docsetName: String,
  httrackArguments: Vector[Shellable] = Vector()
) extends AbstractDocset{
  def name = docsetName

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

  def feed = {    
    val feedXml = 
<entry>
  <version>/{java.time.LocalDateTime.now.toString}</version>
  <url>http://cvogt.org/releases/docsets/{docsetName}.tgz</url>
</entry>

      val feedFile = cwd/'archives/(docsetName++".xml")
      rm(feedFile)
      write(feedFile, feedXml.toString)
  }


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
    %(cwd/'docsets) tar(
      "--exclude='.DS_Store'",
      "-cvzf",
      tarTarget,
      (docsetName++".docset")
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
    mirror( cwd/'temp/docsetName )
    rm( cwd/'html/docsetName )
    mv( cwd/'temp/docsetName/_url.getHost, cwd/'html/docsetName )
    rm( cwd/'temp/docsetName )
  }
  private def mirror(cwd: Path): Unit = {
    val args = Vector[Shellable]( "--mirror", url ) ++ httrackArguments
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
