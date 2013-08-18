package less

import sbt._
import sbt.Keys._
import sbt.Project.{ Initialize, ScopedKey }
//import scala.util.control.NonFatal
import java.io.File
import java.nio.charset.Charset

// 2.10 shim
object NonFatal {
  def apply(t: Throwable): Boolean = t match {
    case _: StackOverflowError => true
    case _: VirtualMachineError | _: ThreadDeath | _: InterruptedException | _: LinkageError  => false
    case _ => true
  }
  def unapply(t: Throwable): Option[Throwable] = if (apply(t)) Some(t) else None
}

/** An sbt plugin interface for lesscss.org 1.3.0 compiler */
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

  private def readFile(file: LessSourceFile, charset: Charset) =
    io.Source.fromFile(file.lessFile)(io.Codec(charset)).mkString

  private def compileSource(
    compiler: lesst.Compile,
    charset: Charset,
    log: Logger)(lessFile: LessSourceFile) =
    try {
      log.debug("Compiling %s" format lessFile)
      compiler(
        lessFile.path, readFile(lessFile, charset))
      .fold({
        case ce: lesst.CompilationError => throw ce
        case NonFatal(e) => throw new RuntimeException(
          "unexpected compilation error: %s" format e.getMessage, e)
      }, {
        case lesst.StyleSheet(css, imports) =>
          IO.write(lessFile.cssFile, css)
          log.debug("Wrote css to file %s" format lessFile.cssFile)
          IO.write(lessFile.importsFile, imports mkString Files.ImportsDelimiter)
          log.debug("Wrote imports to file %s" format lessFile.importsFile)
        lessFile.cssFile
      })
    } catch {
      case NonFatal(e) => throw new RuntimeException(
        "Error occured while compiling %s:\n%s" format(
        lessFile, e.getMessage), e)
    }

  private def allCompilerTask =
    (streams, sourceDirectory in lesskey,
     resourceManaged in lesskey, target in lesskey,
     filter in lesskey, excludeFilter in lesskey,
     charset in lesskey, lessCompiler in lesskey) map compileIf { _ => true }

  private def lessCompileTask =
    (streams, sourceDirectory in lesskey,
     resourceManaged in lesskey, target in lesskey,
     filter in lesskey, excludeFilter in lesskey,
     charset in lesskey, lessCompiler in lesskey) map compileIf(_.changed)

  private def compileIf(cond: LessSourceFile => Boolean)
    (out: std.TaskStreams[ScopedKey[_]], sourcesDir: File, cssDir: File, targetDir: File,
     incl: FileFilter, excl: FileFilter, charset: Charset, compiler: lesst.Compile) =
       (for {
         file <- sourcesDir.descendantsExcept(incl, excl).get
         lessSrc = new LessSourceFile(file, sourcesDir, targetDir, cssDir)
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
    all in lesskey <<= allCompilerTask,
    lessCompiler in lesskey <<= lessCompilerTask
  )
}
