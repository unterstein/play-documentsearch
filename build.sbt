name := "play-documentsearch"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  cache,
  "org.webjars" %% "webjars-play" % "2.2.2-1",
  "org.webjars" % "bootstrap" % "3.1.0",
  "org.webjars" % "bootswatch-superhero" % "3.1.1",
  "org.webjars" % "jquery" % "2.1.1",
  "org.elasticsearch" % "elasticsearch" % "1.2.1",
  "org.elasticsearch" % "elasticsearch-mapper-attachments" % "2.0.0",
  "commons-io" % "commons-io" % "2.4",
  "org.apache.tika" % "tika-core" % "1.5",
  "org.apache.tika" % "tika-parsers" % "1.5",
  "com.google.code.gson" % "gson" % "2.2.4"
)

play.Project.playScalaSettings
