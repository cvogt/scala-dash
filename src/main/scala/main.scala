package org.cvogt.doc
import ammonite.ops._
import org.cvogt.dash._

object OfflineDocsBuilder{
  val docsets = Docsets.docsets
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
