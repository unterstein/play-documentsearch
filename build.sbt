name := "play-documentsearch"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  cache,
  "org.webjars" %% "webjars-play" % "2.2.2-1",
  "org.webjars" % "bootstrap" % "3.1.0",
  "org.webjars" % "bootswatch-superhero" % "3.1.1",
  "org.webjars" % "jquery" % "2.1.1"
)

play.Project.playScalaSettings
