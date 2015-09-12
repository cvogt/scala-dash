import ammonite.ops._
import java.net.URL

object ScalaDashDocsetBuilder{
  val docsets = Vector(
    "Ammonite" -> "http://lihaoyi.github.io/Ammonite/",
    "Fastparse" -> "http://lihaoyi.github.io/fastparse/",
    "ScalaTags" -> "http://lihaoyi.github.io/scalatags/",
    "Scalatex" -> "http://lihaoyi.github.io/Scalatex/",
    "PPrint" -> "http://lihaoyi.github.io/upickle-pprint/pprint/",
    "Hands-on-Scala.js" -> "http://lihaoyi.github.io/hands-on-scala-js/"
  ).map{
    case (name, _url) => new ScalatexDocset( name ){ override def url = _url }
  } ++ Vector(
    new Docset(
      "docs.scala-lang.org",
      Vector("-X/de/","-X/es/","-X/fr/","-X/ja/","-X/ko/","-X/pt-br/","-X/zh-cn/")
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
    new Docset( "cats", Vector("http://plastic-idolatry.com/erik/cats2.png", "-X/cats/api/") ){
      override def url = "http://non.github.io/cats/"
      override def ignore = Vector(
        "Cats", "Page not found · GitHub Pages", "404", "useful links"
      )
    },
    new VersionedDocset(
      Vector("0.2.2").map( v =>
        new Docset( "doobie", Vector(), Some(v) ){
          override def url = s"http://tpolecat.github.io/doobie-$v/00-index.html"
          override def index = docsRootFolder / "00-index.html"
          override def ignore = Vector( "Setting Up" )
          override def selectors = super.selectors.filterNot(Seq("h1","title") contains _._1).map{
            case ("h2",value) => ("h2",Category)
            case other => other
          }
        }
      )
    ),
    new VersionedDocset(
      Vector("3.0.3","3.1.0-RC1","2.1.0").map( v =>
        new Docset( "slick.typesafe.com", Vector(s"-X/doc/$v/api/",s"-X/doc/$v/testkit-api/",s"-X/doc/$v/codegen-api/",s"-X/doc/$v/direct-api/"), Some(v) ){
          override def urlPath = s"doc/$v/" 
          override def ignore = Vector( "[1]","[2]","[3]","[4]","[5]","[6]", "Slick¶", "Search", "SQL¶", "Table Of Contents" )
        }
      )
    ),
    new VersionedDocset(
      Vector("1.2.3").map( v =>
        new Docset( "spray.io", Vector(), Some(v) ){
          override def urlPath = s"documentation/$v/"
          override def ignore = Vector( "Dependencies", "Description", "Example", "Signature" )
        }
      )
    ),
    new VersionedDocset(
      Vector("0.13").map( v =>
        new Docset( "scala-sbt.org", Vector(
          "-I/0.13/",
          "-X/0.13/api/",
          "-X/0.13/tutorial/es/",
          "-X/0.13/tutorial/ja/",
          "-X/0.13/tutorial/zh-cn/",
          "-RCombined+Pages.html"
        ), Some(v) ){
          override def url = "http://www.scala-sbt.org/documentation.html"
          override def ignore = Vector(
            "Community Support", "Commercial Support", "Contents"
          )
          override def index = docsRootFolder / "0.13" / 'docs / "index.html"
          /*
          override def wgetArgs = Vector(
            "--html-extension",
            "--convert-links",
            "--page-requisites",
            "--no-parent",
            "http://www.scala-sbt.org/0.13/docs/Combined+Pages.html",
            "http://www.scala-sbt.org/0.13/tutorial/Combined+Pages.html"
          )
          */
        }
      )
    ),
    new VersionedDocset(
      Vector("2.4.x", "3.0.x", "2.3.x").map( v =>
        new Docset( "playframework.com", Vector(s"-X/documentation/$v/api/"), Some(v) ){
          override def url = s"https://www.playframework.com/documentation/$v/"
          override def ignore = Vector(
            "Documentation", "Books", "Home", "Play releases", "Browse versions", "Browse APIs", "Scala Java Language",
            "Community support", "Professional support"
          )
          override def selectors = super.selectors.filterNot(_._1 startsWith "h").filterNot(_._1 == "title") ++ Map(
            "article h1" -> Category,
            "article h2" -> Section,
            "article h3"  -> Section,
            "article h4" -> Section,
            "article h5"  -> Section,
            "article h6" -> Section
          )
        }
      )
    ),
    new Docset( "learning-scalaz", Vector("-X/learning-scalaz/ja/","-X/learning-scalaz/7.0/") ){
      override def url = "http://eed3si9n.com/learning-scalaz/Combined+Pages.html"
      override def name = "learning Scalaz"
      override def index = docsRootFolder / "Combined+Pages.html"
      override def ignore = Vector("Page not found | eed3si9n")
      override def selectors = super.selectors.filterNot(_._1 == "title").filterNot(_._1 == "h1").map{
        case ("h2",value) => ("h2",Category)
        case other => other
      }
      override val wgetArgs = {
        Vector[Shellable](
          "--convert-links",
          "--page-requisites",
          "-e", "robots=off",
          url
        )
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
      case Array("clean", "all") =>
        rm! cwd/'downloads
        rm! cwd/'docsets
      case Array("download", "clean") => rm! cwd/'downloads
      case Array("build", "clean") => rm! cwd/'docsets
      case Array("download", "all") => docsets.foreach(_.download)
      case Array("build", "all") => docsets.foreach(_.build)
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
      case v: VersionedDocset => v.versions.mkString(" ["," | ","]")
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

class ScalatexDocset(name: String) extends Docset(name){
  override def selectors = super.selectors.filterNot(_._1 == "title").map{
    case ("h1",value) => ("h1",Entry)
    case other => other
  }
  private val digits = (0 to 9).toVector
  override def ignore = Vector("404") ++ (for{
    i <- digits
    j <- digits
    k <- digits
  } yield s"$i.$j.$k")
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
    %(buildFolder) dashing "build"
    archive
    feed
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
    
    %(downloadFolder).applyDynamic("wget")(wgetArgs: _*)
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
