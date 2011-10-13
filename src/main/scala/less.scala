package less

object Plugin extends sbt.Plugin {
  import sbt._
  import sbt.Keys._
  import Project.Initialize._
  import java.nio.charset.Charset
  import java.io.File
  import LessKeys.{less => lesskey, charset, filter, excludeFilter, mini}

  object LessKeys {
    lazy val less = TaskKey[Seq[File]]("less", "Compiles .less sources")
    lazy val mini = SettingKey[Boolean]("mini", "Minifies compiled .less sources")
    lazy val charset = SettingKey[Charset]("charset", "Sets the character encoding used in file IO. Defaults to utf-8")
    lazy val filter = SettingKey[FileFilter]("filter", "Filter for selecting less sources from default directories.")
    lazy val excludeFilter = SettingKey[FileFilter]("exclude-filter", "Filter for excluding files from default directories.")
  }

  type Compiler = { def compile(src: String): Either[String, String] }

  private def css(sources: File, less: File, targetDir: File) =
    Some(new File(targetDir, IO.relativize(sources, less).get.replace(".less",".css")))

  private def compile(compiler: Compiler, charset: Charset, out: Logger)(pair: (File, File)) =
    try {
      val (less, css) = pair
      out.debug("Compiling %s" format less)
      compiler.compile(io.Source.fromFile(less)(
        io.Codec(charset)).mkString).fold({ err =>
        sys.error(err)
      }, { compiled =>
        IO.write(css, compiled)
        out.debug("Wrote to file %s" format css)
        css
      })
    } catch { case e: Exception =>
      throw new RuntimeException(
        "error occured while compiling %s: %s" format(pair._1, e.getMessage), e
      )
    }

  private def compiled(under: File) = (under ** "*.css").get

  private def compileChanged(sources: File, target: File, incl: FileFilter,
                             excl: FileFilter, compiler: Compiler, charset: Charset,
                             log: Logger) =
    (for (less <- sources.descendentsExcept(incl, excl).get;
          css <- css(sources, less, target)
      if (less newerThan css)) yield {
        (less, css)
      }) match {
        case Nil =>
          log.info("No less files to compile")
          compiled(target)
        case xs =>
          log.info("Compiling %d less files to %s" format(xs.size, target))
          xs map compile(compiler, charset, log)
          log.debug("Compiled %s less files" format xs.size)
          compiled(target)
      }

  private def lessCleanTask =
    (streams, resourceManaged in lesskey) map {
      (out, target) =>
        out.log.info("Cleaning generated CSS under " + target)
        IO.delete(target)
    }

  private def lessCompilerTask =
    (streams, sourceDirectory in lesskey, resourceManaged in lesskey,
     filter in lesskey, excludeFilter in lesskey, charset in lesskey, mini in lesskey) map {
      (out, sourceDir, targetDir, incl, excl, charset, mini) =>
        compileChanged(sourceDir, targetDir, incl, excl, compiler(mini), charset, out.log)
    }

  // move defaultExcludes to excludeFilter in unmanagedSources later
  private def lessSourcesTask =
    (sourceDirectory in lesskey, filter in lesskey, excludeFilter in lesskey) map {
      (sourceDir, filt, excl) =>
         sourceDir.descendentsExcept(filt, excl).get
    }

  private def compiler(min: Boolean): Compiler = less.Compiler(min)

  def lessSettingsIn(c: Configuration): Seq[Setting[_]] =
    inConfig(c)(lessSettings0 ++ Seq(
      sourceDirectory in lesskey <<= (sourceDirectory in c) { _ / "less" },
      resourceManaged in lesskey <<= (resourceManaged in c) { _ / "css" },
      resourceGenerators in c <+= lesskey
    )) ++ Seq(
      cleanFiles <+= (resourceManaged in lesskey in c),
      watchSources <++= (unmanagedSources in lesskey in c)
    )

  def lessSettings: Seq[Setting[_]] =
    lessSettingsIn(Compile) ++ lessSettingsIn(Test)

  def lessSettings0: Seq[Setting[_]] = Seq(
    charset in lesskey := Charset.forName("utf-8"),
    mini in lesskey := false,
    filter in lesskey := "*.less",
    // change to (excludeFilter in Global) when dropping support of sbt 0.10.*
    excludeFilter in lesskey := (".*"  - ".") || HiddenFileFilter,
    unmanagedSources in lesskey <<= lessSourcesTask,
    clean in lesskey <<= lessCleanTask,
    lesskey <<= lessCompilerTask
  )
}
