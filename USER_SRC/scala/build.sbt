name := "ssd_blkdev"
scalaVersion := "2.11.7"
organization := "org.janzhou"

enablePlugins(GitVersioning)

scalacOptions ++= Seq("-optimise", "-feature", "-deprecation")

libraryDependencies += "net.java.dev.jna" % "jna" % "4.2.1"
