Script to generate Dash docsets for the documentation of many Scala libraries.

Why? Offline support, fast, unified search

Published as docset feeds here: http://cvogt.org/releases/docsets/

Includes Slick, Spray, Play, Sbt, Ammonite, cats, leaning scalaz

To run this yourself, you need a recent version of wget that supports the --exclude argument. You also need Dashing installed (https://github.com/technosophos/dashing)

Known issues:
The generated sections and categories are far from perfect. See issue #1

How to contribute?
- improve existing docsets
- add docsets for more libraries
- get rid of dashing dependency for more flexibility. See issue #1
