package less

import org.mozilla.javascript.{
  Callable, Context, Function, FunctionObject, JavaScriptException,
  NativeArray, NativeObject, Scriptable, ScriptableObject, ScriptRuntime }
import org.mozilla.javascript.tools.shell.{ Environment, Global }
import java.io.InputStreamReader
import java.nio.charset.Charset

object Compiler {
  type Result = Either[CompilationError, CompilationResult]
}
/** name is required as a reference point for importing relative
 *  dependencies within less */
trait Compiler {
  import Compiler._
  def compile(name: String,
              code: String,
              mini: Boolean = false,
              colors: Boolean = true): Result
}

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
     s.defineFunctionProperties(ShellFunctions,
                                classOf[Global],
                                ScriptableObject.DONTENUM)
     // make rhino `detectable`
     // http://github.com/ringo/ringojs/issues/#issue/88

     Environment.defineClass(s)
     s.defineProperty("environment", new Environment(s),
                      ScriptableObject.DONTENUM)
     s
   }
}

class NativeArrayWrapper(arr: NativeArray) {
  def toList[T](f: AnyRef => T): List[T] =
    arr.getIds map { id =>
      f(arr.get(id.asInstanceOf[java.lang.Integer], null))
    } toList
}

object NativeArrayWrapper {
  implicit def wrapNativeArray(arr: NativeArray): NativeArrayWrapper =
    new NativeArrayWrapper(arr)
}

import NativeArrayWrapper._

case class CompilationResult(cssContent: String, imports: List[String])

class CompilationResultHost extends ScriptableObject {
  var compilationResult: CompilationResult = null

  override def getClassName() = "CompilationResult"

  def jsConstructor(css: String, imports: NativeArray) {
    compilationResult = CompilationResult(css, imports.toList(_.toString))
  }
}

abstract class AbstractCompiler(src: String)
  extends Compiler with ShellEmulation {
  import scala.collection.JavaConversions._

  val utf8 = Charset.forName("utf-8")

  // rhino/less issue: https://github.com/cloudhead/less.js/issues/555

  def compile(
    name: String,
    code: String,
    mini: Boolean = false,
    colors: Boolean = true): Compiler.Result =
    withContext { ctx =>
    try {
      val less = scope.get("compile", scope).asInstanceOf[Callable]
      less.call(ctx, scope, scope, Array(name, code, mini.asInstanceOf[AnyRef]))
      match {
        case cr: CompilationResultHost => Right(cr.compilationResult)
        case ur => Left(UnexpectedResult(ur))
      }
    } catch {
      case e : JavaScriptException =>
        e.getValue match {
          case v: Scriptable =>
            val errorInfo = (Map.empty[String, Any] /: LessError.Properties)(
              (a,e) =>
                if(v.has(e, v)) v.get(e, v) match {
                  case null =>
                    a
                  case na: NativeArray =>
                    a + (e -> na.toArray.map(_.asInstanceOf[Any]).toSeq)
                  case dbl: java.lang.Double
                    if(Seq("line","column", "index").contains(e)) =>
                    a + (e -> dbl.toInt)
                  case job =>
                    a + (e -> job.asInstanceOf[Any])
                } else a
            )
            Left(LessError.from(colors, errorInfo))
          case ue => Left(UnexpectedError(ue)) // null, undefined, Boolean, Number, String, or Function
        }
    }
  }

  override def toString = "%s (%s)" format(super.toString, src)

  private lazy val scope = withContext { ctx =>
    val scope = emulated(ctx.initStandardObjects())
    ctx.evaluateReader(
      scope,
      new InputStreamReader(getClass().getResourceAsStream("/%s" format src),
                            utf8),
      src, 1, null
    )
    ScriptableObject.defineClass(scope, classOf[CompilationResultHost]);
    scope
  }

  private def withContext[T](f: Context => T): T = {
    val ctx = Context.enter()
    try {
      // Do not compile to byte code (max 64kb methods)
      ctx.setOptimizationLevel(-1)
      f(ctx)
    } finally {
      Context.exit()
    }
  }
}

// https://github.com/rolos79/ant-build-script/blob/844bcf74e8d48afda5e5643aa0db8dcd3c125e71/tools/less-rhino-1.3.0.js
// https://github.com/woeye/less.js/blob/b98a6f7537c252d71746862794591e164b0c7238/dist/less-rhino-1.3.0.js

object DefaultCompiler extends AbstractCompiler("less-rhino-1.3.0.js")
