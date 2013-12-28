name := "commons-io-demo"

scalaVersion := "2.9.3"

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.0.1"
  )

scalacOptions += "-deprecation"

releaseSettings

// Publish artifacts to a local repository by default.
publishTo := Some( Resolver.file("file", new File("/Users/gkeramidas/maven-repo/releases")))

initialCommands in console := """
    |import org.apache.commons.io.monitor._
    |import net.keramida.demo.commons.io._
    |""".stripMargin
