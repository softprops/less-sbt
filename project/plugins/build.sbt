addSbtPlugin("me.lessis" % "screen-writer" % "0.1.0-SNAPSHOT")

libraryDependencies <+= sbtVersion(v=>
  "org.scala-tools.sbt" %% "scripted-plugin" % v
)
