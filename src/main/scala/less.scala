package less

object Plugin extends sbt.Plugin {
  import sbt._
  import sbt.Keys._
  import Project.Initialize._
  import java.nio.charset.Charset
  import java.io.File
  import LessKeys.{ less => lesskey, _ }

  object LessKeys {
    lazy val less = TaskKey[Seq[File]]("less", "Compiles .less sources.")
    lazy val mini = SettingKey[Boolean]("mini", "Minifies compiled .less sources. Default is false.")
    lazy val charset = SettingKey[Charset]("charset", "Sets the character encoding used in file IO. Default is utf-8.")
    lazy val filter = SettingKey[FileFilter]("filter", "Filter for selecting less sources from default directories.")
    lazy val colors = SettingKey[Boolean]("colors", "Enables ascii colored output. Default is true")
  }

  private def lessCleanTask =
    (streams, resourceManaged in lesskey) map {
      (out, target) =>
        out.log.info("Cleaning generated CSS under " + target)
        IO.delete(target)
    }

  private def compileSource(
    compiler: Compiler, mini: Boolean,
    colors: Boolean, charset: Charset,
    log: Logger)(lessFile: LessSourceFile) =
    try {
      log.debug("Compiling %s" format lessFile)
      compiler.compile(
        lessFile.path,
        io.Source.fromFile(lessFile.lessFile)(io.Codec(charset)).mkString,
        mini, colors).fold({ err =>
          err match {
            case ce: CompilationError => throw ce
            case e => throw new RuntimeException(
              "unexpected compilation error: %s" format e.getMessage, e)
          }
        }, {
        res =>
          IO.write(lessFile.cssFile, res.cssContent)
          log.debug("Wrote css to file %s" format lessFile.cssFile)
          IO.write(lessFile.importsFile, res.imports mkString Files.ImportsDelimiter)
          log.debug("Wrote imports to file %s" format lessFile.importsFile)
          lessFile.cssFile
      })
    } catch {
      case e => throw new RuntimeException(
        "Error occured while compiling %s:\n%s" format(
        lessFile, e.getMessage), e)
    }

  private def lessCompilerTask =
    (streams, sourceDirectory in lesskey,
     resourceManaged in lesskey, target in lesskey,
     filter in lesskey, excludeFilter in lesskey,
     charset in lesskey, mini in lesskey, colors in lesskey) map {
      (out, sourcesDir, cssDir, targetDir,
       incl, excl, charset, mini, colors) =>
        (for {
            file <- sourcesDir.descendentsExcept(incl, excl).get
            val lessSrc = new LessSourceFile(file, sourcesDir, targetDir, cssDir)
            if lessSrc.changed
         } yield lessSrc) match {
          case Nil =>
            out.log.debug("No less sources to compile")
            compiled(cssDir)
          case files =>
            out.log.info("Compiling %d less sources to %s" format (
              files.size, cssDir))
            files map compileSource(compiler, mini, colors, charset, out.log)
            compiled(cssDir)
        }
    }

  // move defaultExcludes to excludeFilter in unmanagedSources later
  private def lessSourcesTask =
    (sourceDirectory in lesskey, filter in lesskey, excludeFilter in lesskey) map {
      (sourceDir, filt, excl) =>
         sourceDir.descendentsExcept(filt, excl).get
    }

  private def compiler: Compiler = less.DefaultCompiler

  private def compiled(under: File) = (under ** "*.css").get

  def lessSettingsIn(c: Configuration): Seq[Setting[_]] =
    inConfig(c)(lessSettings0 ++ Seq(
      sourceDirectory in lesskey <<= (sourceDirectory in c) { _ / "less" },
      resourceManaged in lesskey <<= (resourceManaged in c) { _ / "css" },
      target in lesskey <<= (target in c) { _ / "less-1.3.0" },
      cleanFiles in lesskey <<= (resourceManaged in lesskey, target in lesskey)(_ :: _ :: Nil),
      watchSources in lesskey <<= (unmanagedSources in lesskey)
    )) ++ Seq(
      cleanFiles <++= (cleanFiles in lesskey in c),
      watchSources <++= (watchSources in lesskey in c),
      resourceGenerators in c <+= lesskey in c,
      compile in c <<= (compile in c).dependsOn(lesskey in c)
    )

  def lessSettings: Seq[Setting[_]] =
    lessSettingsIn(Compile) ++ lessSettingsIn(Test)

  def lessSettings0: Seq[Setting[_]] = Seq(
    charset in lesskey := Charset.forName("utf-8"),
    mini in lesskey := false,
    colors in lesskey := true,
    filter in lesskey := "*.less",
    excludeFilter in lesskey <<= excludeFilter in Global,
    unmanagedSources in lesskey <<= lessSourcesTask,
    clean in lesskey <<= lessCleanTask,
    lesskey <<= lessCompilerTask
  )
}
