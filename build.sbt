lazy val root = (project in file(".")).
  settings(
    name := "EnhancedWatchService",
    version := "0.1",
    javacOptions ++= Seq("-source", "1.8")
  )

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.3"

autoScalaLibrary := false
mainClass in Compile := Some("Main")
