package org.cvogt.doc
import org.cvogt.dash._
import ammonite.ops._

object Docsets{
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
