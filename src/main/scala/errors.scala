package less

sealed trait CompilationError extends RuntimeException

case class UnexpectedResult(result: Any) extends CompilationError {
  override def getMessage =
    "Unexpected javascript return type %s: %s" format(result.getClass, result)
}

case class UnexpectedError(err: Any) extends CompilationError {
  override def getMessage =
    "Unexpected error: %s" format err
}

object LessError {
  val Properties = Seq(
    "name", "message", "type", "filename", "line",
    "column", "callLine", "callExtract", "stack",
    "extract", "index", "filename")

  val UndefVar = """variable (@.*) is undefined""".r

  def from(colors: Boolean, props: Map[String, Any]): CompilationError =
    if (ParseError.is(props)) ParseError(
      props("line").toString.toInt,
      props("column").toString.toInt,
      props("message").toString,
      props("extract").asInstanceOf[Seq[String]],
      colors
    ) else if (SyntaxError.is(props)) SyntaxError(
      props("line").toString.toInt,
      props("column").toString.toInt,
      props("message").toString,
      props("filename").toString,
      props("extract").asInstanceOf[Seq[String]],
      colors
    ) else if(props.isDefinedAt("message")) {
      UndefVar.findFirstMatchIn(props("message").toString) match {
        case Some(mat) =>
          UndefinedVar(
            mat.group(1),
            props("line").toString.toInt,
            props("column").toString.toInt,
            props("extract").asInstanceOf[Seq[String]],
            colors
          )
        case _ => GenericLessError(props)
      }
    } else GenericLessError(props)
}

trait Extracts {
  def showExtract(line: Int, col: Int, extract: Seq[String], colors: Boolean = false) =
    (extract.size.toString.size, extract) match {
      case (pad, Seq(null, at, after)) =>
        ("\n %s %" + pad + "d| %s\n   %" + pad + "d| %s").format(
          err(colors,">"), line, err(colors, at), line + 1, after
        )
      case (pad, Seq(before, at, null)) =>
        ("\n  %" + pad + "d| %s\n%s %" + pad + "d| %s").format(
          line - 1, before, err(colors, ">"), line, err(colors, at)
        )
      case (pad, Seq(before, at, after)) =>
        ("\n  %" + pad + "d| %s\n%s %" + pad + "d| %s\n  %" + pad + "d| %s").format(
          line - 1, before, err(colors, ">"), line, err(colors, at), line + 1, after
        )
      case (pad, ext) =>
        ext.mkString("\n | ", " | %\n", "")
    }
  protected def err(colors: Boolean, str: String) =
    if (colors) Console.RED_B + str + Console.RESET else str
}


object SyntaxError {
  def is(props: Map[String, Any]) =
    (props.isDefinedAt("type") && props("type").equals("Syntax"))
}
case class SyntaxError(line: Int, column: Int, message: String, filename: String, extract: Seq[String], colors: Boolean) extends CompilationError with Extracts {
  override def getMessage =
    "Syntax error on line: %s, column: %s in file %s (%s)%s" format(
      line, column, filename, message, showExtract(line, column, extract, colors)
    )
}

object ParseError {
  def is(props: Map[String, Any]) =
    ((props.isDefinedAt("name") && props("name").equals("ParseError"))
     || props.isDefinedAt("type") && props("type").equals("Parse"))
}
case class ParseError(line: Int, column: Int, message: String, extract: Seq[String], colors: Boolean)
extends CompilationError with Extracts {
  override def getMessage =
    "Parse error on line: %s, column: %s (%s)%s" format(
      line, column, message, showExtract(line, column, extract, colors)
    )
}

case class UndefinedVar(name: String, line: Int, column: Int, extract: Seq[String], colors: Boolean)
extends CompilationError with Extracts {
  override def getMessage = "Undefined variable %s on line: %s, column: %s%s" format(
    err(colors, name), line, column, showExtract(line, column, extract, colors)
  )
}

case class GenericLessError(props: Map[String, Any])
extends CompilationError {
  override def getMessage = "Less error:\n%s" format(props.map {
    case (k, v) => "%s: %s" format(
      k, v match {
        case seq: Seq[_] =>
          seq
        case any =>
          any
      }
    )
  }.mkString("\n"))
}
