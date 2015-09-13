scalaVersion := "2.11.7"
libraryDependencies ++= Seq(
  "com.lihaoyi" %% "ammonite-ops" % "0.4.7", // remember 0.4.7 fails with ensime
  "com.typesafe.play" %% "play-json" % "2.4.3",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.4",
  "org.cvogt" %% "play-json-extensions" % "0.5.0"
)