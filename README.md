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

To overide a setting like `mini`, add something like following to your build file after including the less settings.

    (LessKeys.mini in (Compile, LessKeys.less)) := true

To change the default location of less compiled css files, add the following to your build file

    (resourceManaged in (Compile, LessKeys.less)) <<= (crossTarget in Compile)(_ / "your_preference" / "css")
   
All available keys are exposed through the `LessKeys` module.

## issues 

Have an issue? [Tell me about it](https://github.com/softprops/less-sbt/issues)

## contributions

I'll take them where they make sense. Please use a feature branch in your fork, i.e. git checkout -b my-cool-feature, and if possible, write a [scripted test](http://eed3si9n.com/testing-sbt-plugins) for it.

Doug Tangren (softprops) 2011
