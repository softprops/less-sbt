# less sbt

type [less](http://lesscss.org/) css in your sbt projects

![LESS](http://lesscss.org/images/logo.png) ![Scala](https://github.com/downloads/softprops/coffeescripted-sbt/scala_logo.png)

a friendly companion for [coffeescripted-sbt](https://github.com/softprops/coffeescripted-sbt#readme)

## settings

    less # compiles less source files
    charset(for less) # character encoding used in file IO (defaults to utf-8)
    mini(for less) # setting for compiled minification (false by default)
    filter(for less) # filter for files included by the plugin
    exclude-filter(for less) # filter for files ignored by the plugin
    unmanaged-sources(for less) # lists resolved less sources
    clean(for less) # deletes compiled css
    config:source-directory(for less) # where less files will be resolved from
    config:resource-managed(for less) # where compiled css will be copied to
    
## install it

In your plugin definition add
    
    addSbtPlugin("me.lessis" % "less-sbt" % "0.1.5")
    
And in your build file add

    seq(lessSettings:_*)
    
This will add less settings for the Compile and Test configurations.

To add it to other configurations, use

    seq(lessSettingsIn(SomeOtherConfig):_*)

## use it

Put your `.less` files under `src/main/less` and find the compiled css under `path/to/resource_managed/main/css`

## customize it

### using less's built-in css minification

To override the default `mini` setting, add following to your build definition after including the less settings.

    (LessKeys.mini in (Compile, LessKeys.less)) := true

### changing target css destination

To change the default location of compiled css files, add the following to your build definition

    (resourceManaged in (Compile, LessKeys.less)) <<= (crossTarget in Compile)(_ / "your_preference" / "css")

### working with [@import](http://lesscss.org/#-importing)s

Some less projects, like [Twitter's Bootstrap][bootstrap] project contain one main `.less` file which imports multiple `.less` files using the [@import](http://lesscss.org/#-importing) feature of `less`. To achieve the same kind of compilation with less-sbt, set the `filter` defined by less-sbt to the target of compilation.

   (LessKeys.filter in (Compile, LessKeys.less)) := "your_main.less"

This will build a single `your_main.css` file which includes all of the @imported style definitions.

To see an example of compiling [Bootstrap][bootstrap] itself, check out the [scripted test](https://github.com/softprops/less-sbt/tree/master/src/sbt-test/less-sbt/bootstrap).

Note that using this style of design in combination is sbt's continuous execution operator, `~`, will _not_ trigger less if the primary less file's dependencies change. This is a [known issue](https://github.com/softprops/less-sbt/issues/6).
   
All available keys are exposed through the `LessKeys` module.

## issues 

Have an issue? [Tell me about it](https://github.com/softprops/less-sbt/issues)

## contributions

I'll take them where they make sense. Please use a feature branch in your fork, i.e. git checkout -b my-cool-feature, and if possible, write a [scripted test](http://eed3si9n.com/testing-sbt-plugins) for it.

Doug Tangren (softprops) 2011

[bootstrap]: http://twitter.github.com/bootstrap/
