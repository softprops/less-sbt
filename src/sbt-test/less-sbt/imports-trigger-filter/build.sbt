seq(lessSettings:_*)

// composition of imports
(LessKeys.filter in (Compile, LessKeys.less)) := "main.less"

// imports and things that will trigger the compilation
// of main.less
(LessKeys.importFilter in (Compile, LessKeys.less)) := Some("*.less")
