scalaVersion := "2.11.7"
libraryDependencies ++= Seq(
  "com.lihaoyi" %% "ammonite-ops" % "0.4.6", // remember 0.4.7 fails with ensime
  "com.typesafe.play" %% "play-json" % "2.4.3",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.4",
  "com.typesafe.slick" %% "slick" % "3.1.0-RC1",
  "org.slf4j" % "slf4j-nop" % "1.6.4"
)