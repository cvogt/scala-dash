import ammonite.ops._
import java.net.URL

object ScalaDashDocsetBuilder{
  val docsets = Vector(
    new Docset(
      "docs.scala-lang.org",
      Vector("-*/de/*","-*/es/*","-*/fr/*","-*/ja/*","-*/ko/*","-*/pt-br/*","-*/zh-cn/*")
    ){
      override def ignore = Vector(
        "404",
        "API",
        "Current",
        "Nightly",
        "Learn",
        "Guides & Overviews",
        "Tutorials",
        "Scala Style Guide",
        "Quickref",
        "Glossary",
        "Cheatsheets",
        "Contribute",
        "Source Code",
        "Contributors Guide",
        "Suggestions",
        "Other Resources",
        "Scala Improvement Process"
      )
    },
    new Docset( "Ammonite" ){
      override def url = "http://lihaoyi.github.io/Ammonite/"
      override def ignore = Vector(
        "0.4.6", "0.4.5", "0.4.4", "0.4.3", "0.4.2", "0.4.1", "0.4.0", "0.3.2", "0.3.1", "0.3.0", "0.2.9", "0.2.8", "404"
      )
      override def selectors = super.selectors.filterNot(_._1 == "title")
    },
    new VersionedDocset(
      Vector("3.0.2","3.1.0-M2","2.1.0").map( v =>
        new Docset( "slick.typesafe.com", Vector("-*/api/*","-*-api/*"), Some(v) ){
          override def urlPath = s"doc/$v/" 
          override def ignore = Vector( "[1]","[2]","[3]","[4]","[5]","[6]" )
        }
      )
    ),
    new Docset( "learning-scalaz", Vector("-*/ja/*","-*/7.0/*") ){
      override def url = "http://eed3si9n.com/learning-scalaz/Combined+Pages.html"
      override def name = "learning Scalaz"
      override def index = "Combined+Pages.html"
      override def ignore = Vector("Page not found | eed3si9n")
      override def mirror: Unit = {
        %(downloadFolder)wget(
          "--convert-links",
          "--page-requisites",
          "-e", "robots=off",
          url
        )
      }
      override def selectors = super.selectors.filterNot(_._1 == "title")
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
      case Array("download", "clean") => rm! cwd/'downloads
      case Array("build", "clean") => rm! cwd/'docsets
      case Array("feed", "all") => docsets.foreach(_.feed)
      case Array("download", "all") => docsets.foreach(_.download)
      case Array("build", "all") => docsets.foreach(_.build)
      case Array("feed", docset) => docsets.find(_.docsetName == docset).map(_.feed).getOrElse(usage)
      case Array("download", docset) => docsets.find(_.docsetName == docset).map(_.download).getOrElse(usage)
      case Array("download", docset, version) => docsets.collect{
        case d: VersionedDocset if d.docsetName == docset => d
      }.headOption.flatMap(_.docsets.find(_.version == Some(version))).map(_.download).getOrElse(usage)
      case Array("build", docset) => docsets.find(_.docsetName == docset).map(_.build).getOrElse(usage)
      case Array("build", docset, version) => docsets.collect{
        case d: VersionedDocset if d.docsetName == docset => d
      }.headOption.flatMap(_.docsets.find(_.version == Some(version))).map(_.build).getOrElse(usage)
      case Array() => usage
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

final class VersionedDocset(
  val docsets: Vector[Docset]
) extends AbstractDocset{
  self =>
  val docsetName = {
    assert(docsets.sliding(2).forall{ case Seq(a,b) => a.docsetName == b.docsetName })
    docsets.head.docsetName
  }
  val versions = {
    assert(docsets.forall(_.version.isDefined))
    docsets.flatMap(_.version)
  }

  def download = docsets.map(_.download)
  def build = {
    // reverse, so first version remains as most recent
    docsets.map(_.build)
    val latest = cwd/'docsets/(docsetName + ".tgz")
    rm(latest)
    mkdir(docsets.head.tarFile / RelPath.up)
    cp(docsets.head.tarFile, latest)
  }
  def feed = {    
    val feedXml = 
<entry>
  <version>{versions.head}/{java.time.LocalDateTime.now.toString}</version>
  <url>http://cvogt.org/releases/docsets/{docsetName}.tgz</url>
  <other-versions>{
    versions.map( v =>
      <version><name>{v}</name></version>
    )
  }</other-versions>
</entry>

      val feedFile = cwd/'docsets/(docsetName++".xml")
      mkdir(feedFile/RelPath.up)
      write.over(feedFile, feedXml.toString)
  }
}

class Docset(
  val docsetName: String,
  httrackArguments: Vector[Shellable] = Vector(),
  val version: Option[String] = None
) extends AbstractDocset{
  def url = s"http://$docsetName/$urlPath"
  def urlPath = ""

  // dashing config
  def name = docsetName
  def `package` = "scala"
  def index: String = "index.html"
  def icon32x32 = docsetName ++ ".png"
  def selectors: Map[String,String] = Map(
    "dt a" -> "Command",
    "title" -> "Category",
    "h1" -> "Section",
    "h2" -> "Section",
    "h3"  -> "Section",
    "h4" -> "Section",
    "h5"  -> "Section",
    "h6" -> "Section"
  )
  def ignore: Vector[String] = Vector("ABOUT")
  def allowJs: Boolean = true
  
  final val versionedDocsetName = docsetName++version.map("-"++_).getOrElse("")
  final val downloadFolder = cwd/'downloads/versionedDocsetName
  final def buildFolder = {
    val _url = new URL(url)
    val fileOrDir = downloadFolder / _url.getHost / {
      _url.getPath match {
        case "" | "/" => RelPath.empty
        case path => 
          assert(path(0) == '/')
          RelPath(path.drop(1))
      }
    }
    val info = (stat! fileOrDir)
    if( info.isDir ){
      fileOrDir
    } else if(info.isFile) {
      fileOrDir / RelPath.up
    } else {
      ???
    }
  }
  final val tarFile = cwd / 'docsets / version.map(
    'versions / docsetName / _
  ).getOrElse(
    RelPath.empty
  ) / (docsetName + ".tgz")
  
  final def build = {
    val dashingConfig = buildFolder / "dashing.json"
    val icons = cwd/'icons
    val logo = icons/icon32x32
    if( exists(logo) ){
      rm( buildFolder/icon32x32 )
      cp( logo, buildFolder/icon32x32 )
    } else {
      println("[warning] no icon found for "+docsetName)
    }
    write.over( dashingConfig, json )
    rm(buildFolder / (`package` ++ ".docset"))
    %(buildFolder) dashing "build"
    archive
  }

  final def archive = {
    println(tarFile)
    mkdir( tarFile/RelPath.up )
    rm(tarFile)
    %(buildFolder) tar(
      "--exclude='.DS_Store'",
      "-cvzf",
      tarFile,
      `package` ++ ".docset"
    )
  }

  final def feed = {    
    val feedXml = 
<entry>
  <version>/{java.time.LocalDateTime.now.toString}</version>
  <url>http://cvogt.org/releases/docsets/{docsetName}.tgz</url>
</entry>

    val feedFile = cwd/'docsets/(docsetName++".xml")
    mkdir(feedFile/RelPath.up)
    write.over(feedFile, feedXml.toString)
  }

  final def download: Unit = {
    rm( downloadFolder )
    mkdir( downloadFolder )
    mirror
  }

  def mirror: Unit = {
    val args = Vector[Shellable]( "--mirror", url ) ++ httrackArguments
    %(downloadFolder).applyDynamic("httrack")(args: _*)
  }

  private def json = {
    import play.api.libs.json.{`package` => _,_}
    Json.prettyPrint(
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
  }
}
