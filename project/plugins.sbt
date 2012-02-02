resolvers += "ls-sbt-resolver-0" at "http://repo.lessis.me"

resolvers += "ls-sbt-resolver-1" at "http://repo.codahale.com"

libraryDependencies <+= sbtVersion(v=>
  "org.scala-tools.sbt" %% "scripted-plugin" % v
)

addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.1")
