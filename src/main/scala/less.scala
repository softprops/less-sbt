package less

object Plugin extends sbt.Plugin {
  import sbt._
  import sbt.Keys._
  import Project.Initialize._
  import java.nio.charset.Charset
  import java.io.File
  import LessKeys.{less => lesskey, charset, filter, mini}

  object LessKeys {
    lazy val less = TaskKey[Seq[File]]("less", "Compiles .less sources.")
    lazy val mini = SettingKey[Boolean]("mini", "Minifies compiled .less sources. Defaults to false.")
    lazy val charset = SettingKey[Charset]("charset", "Sets the character encoding used in file IO. Defaults to utf-8.")
    lazy val filter = SettingKey[FileFilter]("filter", "Filter for selecting less sources from default directories.")
  }

  private val ImportsDelimiter = "\n"

  class LessSourceFile(val lessFile: File, sourcesDir: File, targetDir: File, cssDir: File) {
    val relPath = IO.relativize(sourcesDir, lessFile).get

    lazy val cssFile = new File(cssDir, relPath.replaceFirst("\\.less$",".css"))
    lazy val importsFile = new File(targetDir, relPath + ".imports")
    lazy val parentDir = lessFile.getParentFile

    def imports = IO.read(importsFile).split(ImportsDelimiter).collect {
      case fileName if fileName.trim.length > 0 => new File(parentDir, fileName)
    }

    def changed = !importsFile.exists || (lessFile newerThan cssFile) || (imports exists (_ newerThan cssFile))
    def path = lessFile.getPath.replace('\\', '/')

    override def toString = lessFile.toString
  }

  /** name is required as a reference point for importing relative dependencies within less */
  type Compiler = { def compile(name: String, src: String, mini: Boolean): CompilationResult }

  private def lessCleanTask =
    (streams, resourceManaged in lesskey) map {
      (out, target) =>
        out.log.info("Cleaning generated CSS under " + target)
        IO.delete(target)
    }

  private def compileSource(compiler: Compiler, mini: Boolean, charset: Charset, log: Logger)(lessFile: LessSourceFile) = {
    try {
      log.debug("Compiling %s" format lessFile)
      val res = compiler.compile(lessFile.path, io.Source.fromFile(lessFile.lessFile)(io.Codec(charset)).mkString, mini)
      IO.write(lessFile.cssFile, res.cssContent)
      log.debug("Wrote css to file %s" format lessFile.cssFile)
      IO.write(lessFile.importsFile, res.imports mkString ImportsDelimiter)
      log.debug("Wrote imports to file %s" format lessFile.importsFile)
      lessFile.cssFile
    } catch {
      case e => throw new RuntimeException("Error occured while compiling %s: %s" format(lessFile, e.getMessage), e)
    }
  }

  private def lessCompilerTask =
    (streams, sourceDirectory in lesskey, resourceManaged in lesskey, target in lesskey,
     filter in lesskey, excludeFilter in lesskey, charset in lesskey, mini in lesskey) map {
      (out, sourcesDir, cssDir, targetDir, incl, excl, charset, mini) =>
        (for {
            file <- sourcesDir.descendentsExcept(incl, excl).get
            val lessFile = new LessSourceFile(file, sourcesDir, targetDir, cssDir)
            if lessFile.changed
         } yield lessFile) match {
          case Nil =>
            out.log.debug("No less sources to compile")
            compiled(cssDir)
          case files =>
            out.log.info("Compiling %d less sources to %s" format (files.size, cssDir))
            files map compileSource(compiler, mini, charset, out.log)
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
      target in lesskey <<= (target in c) { _ / "less-1.1.5" },
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
    filter in lesskey := "*.less",
    excludeFilter in lesskey <<= excludeFilter in Global,
    unmanagedSources in lesskey <<= lessSourcesTask,
    clean in lesskey <<= lessCleanTask,
    lesskey <<= lessCompilerTask
  )
}
