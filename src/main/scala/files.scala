package less

import java.io.File
import sbt.IO

private [less] object Files {
  val ImportsDelimiter = "\n"
}

class LessSourceMapping(
  val lessFile: File, sourcesDir: File,
  targetDir: File, cssDir: File) {
  import sbt.Path._ // File -> RichFile
  val relPath = IO.relativize(sourcesDir, lessFile).get
  lazy val cssFile = new File(cssDir, relPath.replaceFirst("\\.less$",".css"))
  lazy val importsFile = new File(targetDir, relPath + ".imports")
  lazy val parentDir = lessFile.getParentFile

  def imports = IO.read(importsFile).split(Files.ImportsDelimiter).collect {
    case fileName if fileName.trim.length > 0 => new File(parentDir, fileName)
  }

  def changed =
    (!importsFile.exists
    || (lessFile newerThan cssFile)
    || (imports exists (_ newerThan cssFile)))

  def path = lessFile.getPath.replace('\\', '/')

  override def toString = lessFile.toString
}
