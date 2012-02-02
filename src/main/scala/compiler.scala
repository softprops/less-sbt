package less

import org.mozilla.javascript.{
  Callable, Context, Function, FunctionObject, JavaScriptException,
  NativeObject, Scriptable, ScriptableObject }
import org.mozilla.javascript.tools.shell.{ Environment, Global }
import java.io.InputStreamReader
import java.nio.charset.Charset


object ShellEmulation {
  /** common functions the rhino shell defines */
  val ShellFunctions = Array(
    "doctest",
    "gc",
    "load",
    "loadClass",
    "print",
    "quit",
    "readFile",
    "readUrl",
    "runCommand",
    "seal",
    "sync",
    "toint32",
    "version")
}

/** Most `rhino friendly` js libraries make liberal use
 *  if non emca script properties and functions that the
 *  rhino shell env defines. Unfortunately we are not
 *  evaluating these sources in a rhino shell.
 *  instead of crying me a river, provide an interface
 *  that enables emulation of the shell env */
trait ShellEmulation {
   import ShellEmulation._
   def emulated(s: ScriptableObject) = {
     // define rhino shell functions
     s.defineFunctionProperties(ShellFunctions, classOf[Global], ScriptableObject.DONTENUM)
     // make rhino `detectable` - http://github.com/ringo/ringojs/issues/#issue/88
     Environment.defineClass(s)
     s.defineProperty("environment", new Environment(s), ScriptableObject.DONTENUM)
     s
   }
}

abstract class Compiler(src: String) extends ShellEmulation {
  val utf8 = Charset.forName("utf-8")


  def compile(name: String, code: String, mini: Boolean = false): Either[String, String] = withContext { ctx =>
    try {
      val less = scope.get("compile", scope).asInstanceOf[Callable]
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

  override def toString = "%s (%s)" format(super.toString, src)

  private lazy val scope = withContext { ctx =>
    val scope = emulated(ctx.initStandardObjects())
    ctx.evaluateReader(
      scope,
      new InputStreamReader(getClass().getResourceAsStream("/%s" format src), utf8),
      src, 1, null
    )
    scope
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

object DefaultCompiler extends Compiler("less-rhino-1.1.5.js")
