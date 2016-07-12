name := "ssd_blkdev"
scalaVersion := "2.11.8"
organization := "org.janzhou"

enablePlugins(GitVersioning)

scalacOptions ++= Seq("-optimise", "-feature", "-deprecation")

libraryDependencies ++= Seq(
  "org.janzhou" %% "native" % "0.1.3",
  "com.typesafe" % "config" % "1.2.1",
  "info.debatty" % "java-string-similarity" % "0.13",
  "com.typesafe.akka" %% "akka-actor" % "2.4.1",
  "org.scalanlp" %% "breeze" % "0.12"
)

resolvers += Resolver.jcenterRepo
resolvers += "janzhou-github-mvn-repo" at "https://raw.githubusercontent.com/janzhou/mvn-repo/master"
