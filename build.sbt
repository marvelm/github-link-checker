enablePlugins(ScalaJSPlugin)

name := "Github Link Checker"

scalaVersion := "2.11.7" // or any other Scala version >= 2.10.2

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "0.8.1"
)
