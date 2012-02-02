package less

object Plugin extends sbt.Plugin {
  import sbt._
  import sbt.Keys._
  import Project.Initialize._
  import java.nio.charset.Charset
  import java.io.File
  import LessKeys.{less => lesskey, charset, filter, importFilter, excludeFilter, mini}

  object LessKeys {
    lazy val less = TaskKey[Seq[File]]("less", "Compiles .less sources.")
    lazy val mini = SettingKey[Boolean]("mini", "Minifies compiled .less sources. Defaults to false.")
    lazy val charset = SettingKey[Charset]("charset", "Sets the character encoding used in file IO. Defaults to utf-8.")
    lazy val filter = SettingKey[FileFilter]("filter", "Filter for selecting less sources to compile. When importFilter is None, any stale files attributed to this filter will be compiled.")
     lazy val importFilter = SettingKey[Option[FileFilter]]("import-filter", "Filter for selecting less sources that represent targets of less file imports. When stale, these will trigger the compilation of all files attributed to the filter setting.")
    lazy val excludeFilter = SettingKey[FileFilter]("exclude-filter", "Filter for excluding files from default directories.")
  }

  /** name is required as a reference point for importing relative dependencies within less */
  type Compiler = { def compile(name: String, src: String, mini: Boolean): Either[String, String] }

  type Pair = (File, File)

  private def css(sources: File, less: File, targetDir: File) =
    Some(new File(targetDir, IO.relativize(sources, less).get.replace(".less",".css")))

  private def compileSources(
    compiler: Compiler, mini: Boolean,
    charset: Charset, out: Logger)(pair: Pair) =
    try {
      val (less, css) = pair
      out.debug("Compiling %s" format less)
      compiler.compile(less.getPath, io.Source.fromFile(less)(
        io.Codec(charset)).mkString, mini).fold({ err =>
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

  private def changesIn(
    sources: File, target: File,
    incl: FileFilter, excl: FileFilter) =
    collect(sources, target, incl, excl, {
      (less, css) => less newerThan css
    })_

  private def every(
    sources: File, target: File,
    incl: FileFilter, excl: FileFilter) =
    collect(sources, target, incl, excl, { (_,_) => true })_

  private def collect(
    sources: File, target: File, incl: FileFilter, excl: FileFilter,
    pred: (File, File) => Boolean)(f: Seq[Pair] => Seq[File]) =
      f(for (less <- sources.descendentsExcept(incl, excl).get;
         css <- css(sources, less, target) if (pred(less, css))) yield (less, css))

  private def lessCleanTask =
    (streams, resourceManaged in lesskey) map {
      (out, target) =>
        out.log.info("Cleaning generated CSS under " + target)
        IO.delete(target)
    }

  private def compiling(
    target: File, compiler: Compiler,
    mini: Boolean, charset: Charset, log: Logger)(pairs: Seq[Pair]) =
      pairs match {
        case Nil =>
          log.debug("No less sources to compile")
          compiled(target)
        case xs =>
          log.info("Compiling %d less sources to %s" format(xs.size, target))
          xs map compileSources(compiler, mini, charset, log)
          log.debug("Compiled %s less sources" format xs.size)
          compiled(target)
      }

  private def lessCompilerTask: sbt.Project.Initialize[sbt.Task[Seq[java.io.File]]] =
    (streams, sourceDirectory in lesskey, resourceManaged in lesskey,
     filter in lesskey, importFilter in lesskey, excludeFilter in lesskey,
     charset in lesskey, mini in lesskey) map {
      (out, sourceDir, targetDir, incl, imports, excl, charset, mini) =>
        imports match {
          case Some(imps) =>
            changesIn(sourceDir, targetDir, imps, excl) { chgs =>
              if(!chgs.isEmpty) every(sourceDir, targetDir, incl, excl) (compiling(
                targetDir, compiler, mini, charset, out.log
              )_) else Nil
            }
          case _ =>
            changesIn(sourceDir, targetDir, incl, excl) (compiling(
              targetDir, compiler, mini, charset, out.log
            )_)
        }
    }

  // move defaultExcludes to excludeFilter in unmanagedSources later
  private def lessSourcesTask =
    (sourceDirectory in lesskey, filter in lesskey,
     excludeFilter in lesskey) map {
      (sourceDir, filt, excl) =>
         sourceDir.descendentsExcept(filt, excl).get
    }

  private def compiler: Compiler = less.DefaultCompiler

  def lessSettingsIn(c: Configuration): Seq[Setting[_]] =
    inConfig(c)(lessSettings0 ++ Seq(
      sourceDirectory in lesskey <<= (sourceDirectory in c) { _ / "less" },
      resourceManaged in lesskey <<= (resourceManaged in c) { _ / "css" },
      cleanFiles in lesskey <<= (resourceManaged in lesskey)(_ :: Nil),
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
    importFilter in lesskey := None,
    // change to (excludeFilter in Global) when dropping support of sbt 0.10.*
    excludeFilter in lesskey := (".*"  - ".") || HiddenFileFilter,
    unmanagedSources in lesskey <<= lessSourcesTask,
    clean in lesskey <<= lessCleanTask,
    lesskey <<= lessCompilerTask
  )
}
