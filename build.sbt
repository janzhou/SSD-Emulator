name := "ssd_blkdev"
scalaVersion := "2.11.8"
organization := "org.janzhou"

enablePlugins(GitVersioning)

scalacOptions ++= Seq("-optimise", "-feature", "-deprecation")

libraryDependencies ++= Seq(
  "net.java.dev.jna" % "jna" % "4.2.1",
  "com.typesafe" % "config" % "1.2.1",
  "info.debatty" % "java-string-similarity" % "0.13",
  "com.typesafe.akka" %% "akka-actor" % "2.4.1",
  "org.scalanlp" %% "breeze" % "0.12"
)

resolvers += Resolver.jcenterRepo
