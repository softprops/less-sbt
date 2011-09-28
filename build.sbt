sbtPlugin := true

organization := "me.lessis"

name := "less-sbt"

version := "0.1.0-SNAPSHOT"

libraryDependencies += "rhino" % "js" % "1.7R2"

seq(screenWriterSettings:_*)

seq(scriptedSettings:_*)
