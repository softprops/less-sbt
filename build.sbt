sbtPlugin := true

organization := "me.lessis"

name := "less-sbt"

version <<= sbtVersion(v =>
  if (v.startsWith("0.11") || v.startsWith("0.12") || v.startsWith("0.13")) "0.2.2"
  else error("unsupported sbt version %s" format v)
)

scalacOptions ++= Seq("-deprecation", "-feature")

resolvers += "softprops-maven" at "http://dl.bintray.com/content/softprops/maven"

libraryDependencies += "me.lessis" %% "lesst" % "0.1.2"

seq(scriptedSettings:_*)

scriptedLaunchOpts <<= (scriptedLaunchOpts, version).apply {
  (scriptedOpts, vers) =>
    scriptedOpts ++ Seq(
      "-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + vers
    )
}

scriptedBufferLog := false

seq(lsSettings:_*)

(LsKeys.tags in LsKeys.lsync) := Seq("sbt", "less")

description := "Sbt plugin for compiling Less CSS sources"

// ls bug https://github.com/softprops/ls/issues/54
//(externalResolvers in LsKeys.lsync) <<= (publishTo) map { _.get :: Nil }

homepage <<= (name)( name =>
  Some(url("https://github.com/softprops/%s".format(name))))

licenses <<= (name,version)(
  (name, ver) => Seq("MIT" -> url(
    "https://github.com/softprops/%s/blob/%s/LICENSE" format(name, ver))))

seq(bintraySettings:_*)

publishArtifact in Test := false
