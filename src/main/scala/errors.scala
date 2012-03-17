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
  val UndefVar = """variable (@.*) is undefined""".r
  def from(props: Map[String, Any]): CompilationError =
    if(props.isDefinedAt("name") && props("name").equals("ParseError")) ParseError(
      props("line").toString.toInt,
      props("column").toString.toInt,
      props("message").toString,
      props("extract").asInstanceOf[Seq[String]]
    ) else if(props.isDefinedAt("message")) {
      UndefVar.findFirstMatchIn(props("message").toString) match {
        case Some(mat) =>
          UndefinedVar(
            mat.group(1),
            props("line").toString.toInt,
            props("column").toString.toInt,
            props("extract").asInstanceOf[Seq[String]]
          )
        case _ => GenericLessError(props)
      }
    } else GenericLessError(props)
}

trait Extracts {
  def showExtract(line: Int, col: Int, extract: Seq[String]) =
    (extract.size.toString.size, extract) match {
      case (pad, Seq(null, at, after)) =>
        "\n >%" + pad + "d| %s\n  %" + pad + "d| %s".format(
          line, at, line + 1, after
        )
      case (pad, Seq(before, at, null)) =>
        "\n  %" + pad + "d| %s\n> %" + pad + "d| %s".format(
          line - 1, before, line, at
        )
      case (pad, Seq(before, at, after)) =>
        "\n  %" + pad + "d| %s\n> %" + pad + "d| %s\n  %" + pad + "d| %s".format(
          line - 1, before, line, at, line + 1, after
        )
      case ext => ext.mkString("\n | ", " | %\n", "")
    }
}

case class ParseError(line: Int, column: Int, message: String, extract: Seq[String])
extends CompilationError with Extracts {
  override def getMessage = "Parse error on line: %s, column: %s%s" format(
    line, column, showExtract(line, column, extract)
  )
}

case class UndefinedVar(name: String, line: Int, column: Int, extract: Seq[String])
extends CompilationError with Extracts {
  override def getMessage = "Undefined variable %s on line: %s, column: %s%s" format(
    name, line, column, showExtract(line, column, extract)
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
