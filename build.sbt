sbtPlugin := true

organization := "me.lessis"

name := "less-sbt"

sbtVersion in Global := "0.13.0"

scalaVersion in Global := "2.10.2"

version <<= sbtVersion(v =>
  if (v.startsWith("0.11") || v.startsWith("0.12") || v.startsWith("0.13")) "0.2.2-SNAPSHOT"
  else error("unsupported sbt version %s" format v)
)

scalacOptions ++= Seq("-deprecation")//, "-feature")

resolvers += "softprops-maven" at "http://dl.bintray.com/content/softprops/maven"

libraryDependencies += "me.lessis" %% "lesst" % "0.1.2-SNAPSHOT"

seq(scriptedSettings:_*)

//scriptedLaunchOpts := { scriptedLaunchOpts.value ++
//  Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value)
//}

scriptedBufferLog := false

seq(lsSettings:_*)

(LsKeys.tags in LsKeys.lsync) := Seq("sbt", "less")

description := "Sbt plugin for compiling Less CSS sources"

// ls bug https://github.com/softprops/ls/issues/54
//(externalResolvers in LsKeys.lsync) <<= (publishTo) map { _.get :: Nil }

homepage <<= (name)( name =>
  Some(url("https://github.com/softprops/%s".format(name))))

publishTo := Some(Classpaths.sbtPluginReleases) 

publishMavenStyle := false

publishArtifact in Test := false

licenses <<= (name,version)(
  (name, ver) => Seq("MIT" -> url(
    "https://github.com/softprops/%s/blob/%s/LICENSE" format(name, ver))))

pomExtra := (
  <scm>
    <url>git@github.com:softprops/less-sbt.git</url>
    <connection>scm:git:git@github.com:softprops/less-sbt.git</connection>
  </scm>
  <developers>
    <developer>
      <id>softprops</id>
      <name>Doug Tangren</name>
      <url>https://github.com/softprops</url>
    </developer>
  </developers>
)
