// See README.md for license details.

ThisBuild / scalaVersion     := "2.13.10"
ThisBuild / version          := "0.1.0"
ThisBuild / organization     := "com.github.sterin"

val options = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-unchecked",
  // "-Xfatal-warnings",
  "-language:reflectiveCalls",
  "-Ymacro-annotations",
  "-Wconf:msg=firrtl:s",
)

val chiselVersion = "3.6.0"
val chiselTestVersion = "0.6.0"

lazy val root = (project in file("."))
  .settings(
    name := "chisel-tests-1",
    libraryDependencies ++= Seq(
      "edu.berkeley.cs" %% "chisel3" % chiselVersion,
      "edu.berkeley.cs" %% "chiseltest" % chiselTestVersion % "test"
    ),

    scalacOptions ++= options,
    addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % chiselVersion cross CrossVersion.full)
  )
