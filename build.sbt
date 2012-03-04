sbtPlugin := true

organization := "me.lessis"

name := "less-sbt"

version <<= sbtVersion(v =>
  if(v.startsWith("0.11")) "0.1.10-SNAPSHOT"
  else error("unsupported sbt version %s" format v)
)

libraryDependencies += "org.mozilla" % "rhino" % "1.7R3"

seq(scriptedSettings:_*)

seq(lsSettings:_*)

(LsKeys.tags in LsKeys.lsync) := Seq("sbt", "less")

(description in LsKeys.lsync) :=
  "Sbt plugin for compiling Less CSS sources"

// ls bug https://github.com/softprops/ls/issues/54
//(externalResolvers in LsKeys.lsync) <<= (publishTo) map { _.get :: Nil }

homepage :=
  Some(url("https://github.com/softprops/less-sbt"))

publishTo := Some(Resolver.url("sbt-plugin-releases", url(
  "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"
))(Resolver.ivyStylePatterns))

publishMavenStyle := false

publishArtifact in Test := false

licenses := Seq("MIT" -> url(
  "https://github.com/softprops/less-sbt/blob/0.1.9/LICENSE"))

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
