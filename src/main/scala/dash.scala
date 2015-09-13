package org.cvogt.dash

import ammonite.ops._
import java.net.URL
import play.api.libs.json._
import org.cvogt.play.json._
import org.cvogt.play.json.implicits.optionWithNull
import scala.collection.immutable.ListMap

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
    assert(docsets.sliding(2).forall{
      case Seq(_) => true
      case Seq(a,b) => a.docsetName == b.docsetName
    })
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
    feed
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

case class DashingConfig(
  name: String,
  `package`: String,
  index: String,
  selectors: Map[String,SelectorTarget],
  ignore: Vector[String],
  icon32x32: String,
  allowJs: Boolean
){
  require(selectors.isInstanceOf[ListMap[_,_]])
}

class Docset(
  val docsetName: String,
  wgetExtraArgs: Vector[Shellable] = Vector(),
  val version: Option[String] = None
) extends AbstractDocset{
  def url = s"http://$docsetName/$urlPath"
  def urls = Vector(url)
  val wgetArgs = Seq[Shellable](
    "--mirror",
    "--html-extension",
    "--convert-links",
    "--page-requisites",
    "--no-parent",
    url
  ) ++ wgetExtraArgs
  def urlPath = ""
  def now = java.time.ZonedDateTime.now.withZoneSameInstant(java.time.ZoneId.of("UTC"))

  // dashing config
  def name = docsetName
  def `package` = "scala"
  def index: RelPath = docsRootFolder / "index.html"
  def icon32x32 = docsetName ++ ".png"
  def selectors: ListMap[String,SelectorTarget] =
  ListMap(
    (1 to 6).map("h"+_).flatMap(
    h => Vector(
      h+":matches(^[^a-zA-Z]+$)" -> Command,
      h+":matches(^([a-z]|[A-Z])$)" -> Shortcut,
      h+":matches(^([a-z][a-z]+|[A-Z][A-Z]+)$)" -> Command,
      h+":not(:matches(^([a-z]+|[A-Z]+|[^a-zA-Z]+)$))" -> Section
    )
  ):_*) ++ ListMap(
    "dt a" -> Command,
    "title" -> Guide
  )
  def ignore: Vector[String] = Vector("ABOUT")
  def allowJs: Boolean = true
  
  final val versionedDocsetName = docsetName++version.map("-"++_).getOrElse("")
  final val downloadFolder = cwd/'downloads/versionedDocsetName
  final def buildFolder = downloadFolder

  final def docsRootFolder = {
    val _url = new URL(url)
    val fileOrDir = _url.getHost / {
      _url.getPath match {
        case "" | "/" => RelPath.empty
        case path => 
          assert(path(0) == '/')
          RelPath(path.drop(1))
      }
    }
    if( exists(downloadFolder / fileOrDir) ){    
      val info = stat! (downloadFolder / fileOrDir)
      if( info.isDir ){
        fileOrDir
      } else if(info.isFile) {
        fileOrDir / RelPath.up
      } else {
        ???
      }
    } else {
      fileOrDir / RelPath.up
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
    %.dashing("build")(buildFolder)
    archive
    feed
  }

  final def archive = {
    println(tarFile)
    mkdir( tarFile/RelPath.up )
    rm(tarFile)
    %.tar(
      "--exclude='.DS_Store'",
      "-cvzf",
      tarFile,
      `package` ++ ".docset"
    )(buildFolder)
  }

  final def feed = {    
    val feedXml = 
<entry>
  <version>{version.getOrElse(now.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE))}/{now.toString}</version>
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
    /*val args = Vector[Shellable]( "--mirror", "-n", url ) ++ httrackArguments
    %(downloadFolder).applyDynamic("httrack")(args: _*)*/
    
    %.applyDynamic("wget")(wgetArgs: _*)(downloadFolder)
  }

  private def json = {
    import play.api.libs.json.{`package` => _,_}
    val config = DashingConfig(
      name, `package`, index.toString, selectors, ignore, icon32x32, allowJs
    )
    Json.prettyPrint( Json.toJson( config ) )
  }
}

object DashingConfig{
  implicit val jsonFormat: Format[DashingConfig] = Json.format[DashingConfig]
}
