sbtPlugin := true

organization := "me.lessis"

name := "less-sbt"

version <<= sbtVersion(v =>
  if(v.startsWith("0.11")) "0.1.0"
  else if (v.startsWith("0.10")) "0.1.0-%s".format(v)
  else error("unsupported sbt version %s" format v)
)

libraryDependencies += "rhino" % "js" % "1.7R2"

seq(scriptedSettings:_*)
