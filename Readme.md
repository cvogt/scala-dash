Docset feeds: http://cvogt.org/releases/docsets/

Script to generate Dash docsets for the manuals (not API docs) of many Scala libraries.

Why? Offline support, fast, unified search. API was already available via maven, manuals weren't.

Includes
[Scala](http://docs.scala-lang.org/), 
[Slick](http://slick.typesafe.com/docs/),
[Spray](http://spray.io/documentation/),
[Playframework](https://www.playframework.com/documentation/),
[Sbt](http://www.scala-sbt.org/documentation.html),
[Ammonite](https://lihaoyi.github.io/Ammonite/),
[cats](https://non.github.io/cats/),
[leaning scalaz](http://eed3si9n.com/learning-scalaz/),
[doobie](http://tpolecat.github.io/doobie-0.2.2/00-index.html),
[fastparse](http://lihaoyi.github.io/fastparse/),
[Hand-on Scala.js](http://lihaoyi.github.io/hands-on-scala-js/),
[PPrint](http://lihaoyi.github.io/upickle-pprint/pprint/),
[ScalaTags](http://lihaoyi.github.io/scalatags/),
[Scalatex](http://lihaoyi.github.io/Scalatex/),
[uPickle](http://lihaoyi.github.io/upickle-pprint/upickle/)

To run this yourself, you need a recent version of wget that supports the --exclude argument. You also need Dashing installed (https://github.com/technosophos/dashing)

How to contribute?
- improve existing docsets: Tweak css selectors, add ignores, improve text via regexes
- add docsets for more libraries
