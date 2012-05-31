seq(lessSettings:_*)

// one file does it all
// https://github.com/twitter/bootstrap/blob/v2.0.0/Makefile#L15

(LessKeys.filter in (Compile, LessKeys.less)) := "bootstrap.less"

InputKey[Unit]("contents") <<= inputTask { (argsTask: TaskKey[Seq[String]]) =>
  (argsTask, streams) map {
    (args, out) =>
      args match {
        case Seq(given, expected) =>
          if(IO.read(file(given)).trim.equals(IO.read(file(expected)).trim)) out.log.debug(
            "Contents match"
          )
          else {
            println(IO.read(file(expected)))
            error(
            "Contents of (%s)\n%s does not match (%s)\n%s" format(
              given, IO.read(file(given)), expected, IO.read(file(expected))
            )
          )
          }
      }
  }
}
