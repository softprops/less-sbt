package less

import sbt._
import sbt.Keys._
import sbt.Def.{ Initialize, ScopedKey }
import java.io.File
import java.nio.charset.Charset

/** 2.10 shim for classifying Non fatal exceptions in exception handling */
private [less] object NonFatal {
  def apply(t: Throwable): Boolean = t match {
    case _: StackOverflowError => true
    case _: VirtualMachineError | _: ThreadDeath | _: InterruptedException | _: LinkageError  => false
    case _ => true
  }
  def unapply(t: Throwable): Option[Throwable] = if (apply(t)) Some(t) else None
}

/** Sbt frontend for the less CSS compiler */
object Plugin extends sbt.Plugin {

  object LessKeys {
    lazy val less = TaskKey[Seq[File]](
      "less", "Compiles .less sources.")
    lazy val mini = SettingKey[Boolean](
      "mini", "Minifies compiled .less sources. Default is false.")
    lazy val charset = SettingKey[Charset](
      "charset", "Sets the character encoding used in file IO. Default is utf-8.")
    lazy val filter = SettingKey[FileFilter](
      "filter", "Filter for selecting less sources from default directories.")
    lazy val colors = SettingKey[Boolean](
      "colors", "Enables ascii colored output. Default is true")
    lazy val all = TaskKey[Seq[File]](
      "all", "Compiles all .less sources regardless of freshness")
    lazy val lessCompiler = TaskKey[lesst.Compile](
      "lessCompiler", "The compiler used for compiling .less sources")
  }
  import LessKeys.{ less => lesskey, _ }

  private def lessCleanTask =
    (streams, resourceManaged in lesskey) map {
      (out, target) =>
        out.log.info("Cleaning generated CSS under %s" format target)
        IO.delete(target)
    }

  private def readFile(mapping: LessSourceMapping, charset: Charset) =
    io.Source.fromFile(mapping.lessFile)(io.Codec(charset)).mkString

  private def compileSource(
    compiler: lesst.Compile,
    charset: Charset,
    log: Logger)(mapping: LessSourceMapping) =
    try {
      log.debug("Compiling %s" format mapping.lessFile)
      compiler(
        mapping.path, readFile(mapping, charset))
      .fold({
        case ce: lesst.CompilationError => throw ce
        case NonFatal(e) => throw new RuntimeException(
          "unexpected less css compilation error: %s" format e.getMessage, e)
      }, {
        case lesst.StyleSheet(css, imports) =>
          IO.write(mapping.cssFile, css)
          log.debug("Wrote css to file %s" format mapping.cssFile)
          IO.write(mapping.importsFile, imports mkString Files.ImportsDelimiter)
          log.debug("Wrote imports to file %s" format mapping.importsFile)
          mapping.cssFile
      })
    } catch {
      case NonFatal(e) => throw new RuntimeException(
        "Error occured while compiling %s:\n%s" format(
        mapping, e.getMessage), e)
    }

  private def allCompileTask =
    (streams, sourceDirectory in lesskey,
     resourceManaged in lesskey, target in lesskey,
     filter in lesskey, excludeFilter in lesskey,
     charset in lesskey, lessCompiler in lesskey) map compileIf { _ => true }

  private def lessCompileTask =
    (streams, sourceDirectory in lesskey,
     resourceManaged in lesskey, target in lesskey,
     filter in lesskey, excludeFilter in lesskey,
     charset in lesskey, lessCompiler in lesskey) map compileIf(_.changed)

  private def compileIf(cond: LessSourceMapping => Boolean)
    (out: std.TaskStreams[ScopedKey[_]], sourcesDir: File, cssDir: File, targetDir: File,
     incl: FileFilter, excl: FileFilter, charset: Charset, compiler: lesst.Compile) =
       (for {
         file <- sourcesDir.descendantsExcept(incl, excl).get
         lessSrc = new LessSourceMapping(file, sourcesDir, targetDir, cssDir)
         if cond(lessSrc)
       } yield lessSrc) match {
         case Nil =>
           out.log.debug("No less sources to compile")
           compiled(cssDir)
         case files =>
           out.log.info("Compiling %d less sources to %s" format (
           files.size, cssDir))
           files map compileSource(compiler, charset, out.log)
           compiled(cssDir)
       }

  // move defaultExcludes to excludeFilter in unmanagedSources later
  private def lessSourcesTask =
    (sourceDirectory in lesskey, filter in lesskey, excludeFilter in lesskey) map {
      (sourceDir, filt, excl) =>
         sourceDir.descendantsExcept(filt, excl).get
    }

  private def lessCompilerTask: Initialize[sbt.Task[lesst.Compile]] =
    (mini in lesskey, colors in lesskey) map {
      (min, clrs) => lesst.Compile(lesst.Options(mini = min, colors = clrs))
    }

  private def compiled(under: File) = (under ** "*.css").get

  def lessSettingsIn(c: Configuration): Seq[Setting[_]] =
    inConfig(c)(lessSettings0 ++ Seq(
      sourceDirectory in lesskey <<= (sourceDirectory in c) { _ / "less" },
      resourceManaged in lesskey <<= (resourceManaged in c) { _ / "css" },
      target in lesskey <<= (target in c) { _ / "less-1.4.2" },
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
    lesskey <<= lessCompileTask,
    all in lesskey <<= allCompileTask,
    lessCompiler in lesskey <<= lessCompilerTask
  )
}
