
# less sbt

type [less](http://lesscss.org/) css in your sbt projects

![LESS](http://lesscss.org/images/logo.png) ![Scala](https://github.com/downloads/softprops/coffeescripted-sbt/scala_logo.png)

a friendly css companion for [coffeescripted-sbt][coffeescript] using the less 1.4.2 embedded compiler via [lesst](https://github.com/softprops/lesst#readme).

## settings

For sbt 0.12 users

    all(for less) # compiles all less source files regardless of freshness
    less # compiles less source files
    charset(for less) # character encoding used in file IO (defaults to utf-8)
    mini(for less) # setting for compiled minification (false by default)
    colors(for less) # setting for color error output (true by default)
    lessCompiler(for less) # task for resolving the less compiler to compile .less sources
    filter(for less) # filter for files included by the plugin
    exclude-filter(for less) # filter for files ignored by the plugin
    unmanaged-sources(for less) # lists resolved less sources
    clean(for less) # deletes compiled css
    config:source-directory(for less) # where less files will be resolved from
    config:resource-managed(for less) # where compiled css will be copied to
    
For sbt 0.13 users, the syntax slightly changed. setting keys are now camel cased from the REPL and are accessed when prefiexed with `less::`.


## install it

In your plugin definition, add

```scala    
addSbtPlugin("me.lessis" % "less-sbt" % "0.2.0")
```
   
For sbt 0.13 users add the following resolver to your plugin configuration

```scala
resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
      url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(
              Resolver.ivyStylePatterns)
```
    
Then in your build definition, add

```scala
seq(lessSettings:_*)
```
    
This will append `less-sbt`'s settings for the `Compile` and `Test` configurations.

To add them to other configurations, use the provided `lessSettingsIn(config)` method.

```scala
seq(lessSettingsIn(SomeOtherConfig):_*)
```

## use it

Author your `.less` files under your project's `src/main/less` directory. After compiling less sources, you can find the compiled css under `path/to/resource_managed/main/css`

## customize it

### using less's built-in css minification

Less css, itself, provides a built-in minifier which you can to to shink your compiled css. To override the default `mini` setting, add following to your build definition after including the less settings.

```scala
(LessKeys.mini in (Compile, LessKeys.less)) := true
```

### changing target css destination

To change the default location of compiled css files, add the following to your build definition

```scala
(resourceManaged in (Compile, LessKeys.less)) <<= (crossTarget in Compile)(_ / "your_preference" / "css")
```

### working with [@import][importing]s

Some lesscss projects, like [Twitter's Bootstrap][bootstrap] project contain one main `.less` file which imports multiple `.less` files using the [@import][importing] feature of lesscss. To achieve the same style of compilation with less-sbt, set the `filter` defined by less-sbt to the target of compilation.

```scala
(LessKeys.filter in (Compile, LessKeys.less)) := "your_main.less"
```

This will build a single `your_main.css` file which includes all of the @imported style definitions.

To see an example of compiling [Bootstrap][bootstrap] itself, check out the [scripted bootstrap test](https://github.com/softprops/less-sbt/tree/master/src/sbt-test/less-sbt/bootstrap).
   
You will find all custom `less-sbt` keys within the `LessKeys` module.

## issues 

Have an issue? [Tell me about it][issues]

## contributions

I'll take them where they make sense. Please use a feature branch in your fork, i.e. git checkout -b my-cool-feature, and if possible, write a [scripted test](http://eed3si9n.com/testing-sbt-plugins) for it.

Doug Tangren (softprops) 2011-2013

[issues]: https://github.com/softprops/less-sbt/issues
[importing]: http://lesscss.org/#-importing
[bootstrap]: http://twitter.github.com/bootstrap/
[coffeescript]: https://github.com/softprops/coffeescripted-sbt#readme
