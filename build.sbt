sbtPlugin := true

organization := "me.lessis"

name := "less-sbt"

version <<= sbtVersion(v =>
  if(v.startsWith("0.11")) "0.1.1-SNAPSHOT"
  else if (v.startsWith("0.10")) "0.1.1-%s-SNAPSHOT".format(v)
  else error("unsupported sbt version %s" format v)
)

libraryDependencies += "rhino" % "js" % "1.7R2"

publishTo :=  Some(Resolver.file("lessis repo", new java.io.File("/var/www/repo")))

seq(scriptedSettings:_*)
