V23GoRunner
===========

The V23GoRunner android app allows running arbitrary Vanadium Go
code in the android application.

Running your Vanadium Go Code in Android
----------------------------------------

1. Modify the [V23GoRunnerFuncs] map to include your custom Go function, keyed by a unique
   string key.

2. Build the Vanadium android-lib library and publish it to maven local.

```sh
cd $JIRI_ROOT/release/java
./gradlew :lib:clean :lib:publishToMavenLocal
./gradlew :android-lib:clean :android-lib:publishToMavenLocal
```

You may need to run `jiri goext distclean` for your changes to take effect.

3. In Android Studio, where you have opened the V23GoRunner application,
   run "Build >> Clean project" to remove any locally cached versions
   of the Vanadium android-lib.

4. In Android Studio, run the application. It will display a text field. Inputting your unique
   string key into the field and hitting "Run" will run your custom Go code.

[V23GoRunnerFuncs]: https://vanadium.googlesource.com/release.go.x.jni/+/master/impl/google/services/v23_go_runner/funcs.go
