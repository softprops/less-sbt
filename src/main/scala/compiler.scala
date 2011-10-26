package less

import org.mozilla.javascript.{Callable, Context, Function, FunctionObject, JavaScriptException, NativeObject, Scriptable}
import org.mozilla.javascript._
import java.io.InputStreamReader
import java.nio.charset.Charset

case class Compiler(mini: Boolean = false) {
   val utf8 = Charset.forName("utf-8")

  def compile(name: String, code: String): Either[String, String] = withContext { ctx =>
    val scope = ctx.initStandardObjects()
    ctx.evaluateReader(scope,
      new InputStreamReader(getClass().getResourceAsStream("/less-rhino-1.1.3.js"), utf8),
     "less-rhino-1.1.3.js", 1, null
    )

   val less = scope.get("compile", scope).asInstanceOf[Callable]

    try {
      Right(less.call(ctx, scope, scope, Array(name, code, mini.asInstanceOf[AnyRef])).toString)
    } catch {
      case e : JavaScriptException =>
        e.getValue match {
          case v: Scriptable =>
            Left(ScriptableObject.getProperty(v, "message").toString)
          case v => sys.error("unknown exception value type %s" format v)
        }
    }
  }

  private def withContext[T](f: Context => T): T = {
    val ctx = Context.enter()
    try {
      ctx.setOptimizationLevel(-1) // Do not compile to byte code (max 64kb methods)
      f(ctx)
    } finally {
      Context.exit()
    }
  }
}
