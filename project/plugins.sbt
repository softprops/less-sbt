resolvers += Resolver.url("scalasbt", new URL(
  "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases"))(
  Resolver.ivyStylePatterns)

addSbtPlugin("com.jsuereth" % "xsbt-gpg-plugin" % "0.6")

resolvers ++= Seq(
  "less is" at "http://repo.lessis.me",
  "coda" at "http://repo.codahale.com"
)

libraryDependencies <+= sbtVersion(v=>
  v.split('.') match {
    case Array("0", "11", "2") =>
      "org.scala-tools.sbt" %% "scripted-plugin" % v
    case e =>
      "org.scala-sbt" %% "scripted-plugin" % v
  }
)

addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.1")
