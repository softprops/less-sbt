resolvers ++= Seq(
  "less is" at "http://repo.lessis.me",
  "coda" at "http://repo.codahale.com"
)

libraryDependencies <+= sbtVersion(v=>
  "org.scala-tools.sbt" %% "scripted-plugin" % v
)

addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.1")
