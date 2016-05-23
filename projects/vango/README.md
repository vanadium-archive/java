# VAnGo

The VAnGo (Vanadium-Android-Go) android app allows running arbitrary Vanadium Go
code in the android application.

## Building Vanadium Go Code for Android

1. Modify the [vangoFuncs] map to include your custom Go function, keyed by a unique
   string key.

2. Build the Vanadium android-lib library and publish it to maven local.

```sh
cd $JIRI_ROOT/release/java
jiri goext distclean
./gradlew :lib:clean :android-lib:clean :lib:publishToMavenLocal :android-lib:publishToMavenLocal
```

## Running using Android Studio

1. In Android Studio, where you have opened the VAnGo application,
   run "Build >> Clean project" to remove any locally cached versions
   of the Vanadium android-lib.

2. In Android Studio, run the application. It will display a text field. Inputting your unique
   string key into the field and hitting "Run" will run your custom Go code.

## Running using [`madb`]

1. Set the `ANDROID_HOME` environment variable

2. Build, run and read logs

```sh
madb start
madb exec logcat *:S vlog:*
```

[vangoFuncs]: https://vanadium.googlesource.com/release.go.x.jni/+/master/impl/google/services/vango/funcs.go
[`madb`]: https://github.com/vanadium/madb
