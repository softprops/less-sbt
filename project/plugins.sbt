resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
    url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(
        Resolver.ivyStylePatterns)

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.1.1")

libraryDependencies <+= sbtVersion(v =>"org.scala-sbt" % "scripted-plugin" % v)

addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.3")
