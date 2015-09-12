package org.cvogt.dash

import ammonite.ops._
import java.net.URL

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
  def selectors: Map[String,EntryType] = Map(
    "dt a" -> Command,
    "title" -> Category,
    "h1" -> Section,
    "h2" -> Section,
    "h3"  -> Section,
    "h4" -> Section,
    "h5"  -> Section,
    "h6" -> Section
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
    val info = stat! (downloadFolder / fileOrDir)
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
    Json.prettyPrint(
      Json.obj(
        "name" -> JsString(name),
        "package" -> JsString(`package`),
        "index" -> JsString(index.toString),
        "selectors" -> JsObject(selectors.mapValues(e => JsString(e.toString))),
        "ignore" -> JsArray(ignore.map(JsString(_))),
        "icon32x32" -> JsString(icon32x32),
        "allowJs" -> JsBoolean(allowJs)
      )
    )
  }
}

sealed trait EntryType
case object Annotation extends EntryType
case object Attribute extends EntryType
case object Binding extends EntryType
case object Builtin extends EntryType
case object Callback extends EntryType
case object Category extends EntryType
case object Class extends EntryType
case object Command extends EntryType
case object Component extends EntryType
case object Constant extends EntryType
case object Constructor extends EntryType
case object Define extends EntryType
case object Delegate extends EntryType
case object Diagram extends EntryType
case object Directive extends EntryType
case object Element extends EntryType
case object Entry extends EntryType
case object Enum extends EntryType
case object Environment extends EntryType
case object Error extends EntryType
case object Event extends EntryType
case object Exception extends EntryType
case object Extension extends EntryType
case object Field extends EntryType
case object File extends EntryType
case object Filter extends EntryType
case object Framework extends EntryType
case object Function extends EntryType
case object Global extends EntryType
case object Guidecase extends EntryType
case object Hook extends EntryType
case object Instance extends EntryType
case object Instruction extends EntryType
case object Interface extends EntryType
case object Keyword extends EntryType
case object Library extends EntryType
case object Literal extends EntryType
case object Macro extends EntryType
case object Method extends EntryType
case object Mixin extends EntryType
case object Modifier extends EntryType
case object Module extends EntryType
case object Namespace extends EntryType
case object Notation extends EntryType
case object Object extends EntryType
case object Operator extends EntryType
case object Option extends EntryType
case object Package extends EntryType
case object Parameter extends EntryType
case object Plugin extends EntryType
case object Procedure extends EntryType
case object Property extends EntryType
case object Protocol extends EntryType
case object Provider extends EntryType
case object Provisioner extends EntryType
case object Query extends EntryType
case object Record extends EntryType
case object Resource extends EntryType
case object Sample extends EntryType
case object Section extends EntryType
case object Service extends EntryType
case object Setting extends EntryType
case object Shortcut extends EntryType
case object Statement extends EntryType
case object Struct extends EntryType
case object Style extends EntryType
case object Subroutine extends EntryType
case object Tag extends EntryType
case object Test extends EntryType
case object Trait extends EntryType
case object Type extends EntryType
case object Union extends EntryType
case object Value extends EntryType
case object Variable extends EntryType
case object Wordcase extends EntryType
