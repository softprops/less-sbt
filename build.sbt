sbtPlugin := true

organization := "me.lessis"

name := "less-sbt"

version <<= sbtVersion(v =>
  if(v.startsWith("0.11")) "0.1.5-SNAPSHOT"
  else error("unsupported sbt version %s" format v)
)

libraryDependencies += "rhino" % "js" % "1.7R2"

publishTo := Some("Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/releases/")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

seq(scriptedSettings:_*)

seq(lsSettings:_*)

(LsKeys.tags in LsKeys.lsync) := Seq("sbt", "less")

(description in LsKeys.lsync) :=
  "Sbt plugin for compiling Less CSS sources"

(licenses in LsKeys.lsync) := Seq(
  ("MIT", url("https://github.com/softprops/less-sbt/blob/0.1.4/LICENSE"))
)
