# Contributing to Vanadium

Vanadium is an open source project.

It is the work of many contributors. We appreciate your help!

## Filing issues

We use a single GitHub repository for [tracking all
issues](https://github.com/vanadium/issues/issues) across all Vanadium
repositories.

## Contributing code

Please read the [contribution
guidelines](https://github.com/vanadium/docs/blob/master/contributing/README.md)
before sending patches.

**We do not accept GitHub pull requests.** (We use
[Gerrit](https://www.gerritcodereview.com/) instead for code reviews.)

Unless otherwise noted, the Vanadium source files are distributed under the
BSD-style license found in the LICENSE file.

## Testing changes

Typical users of the Vanadium for Java libraries will use a dependency manager
such as Maven or Gradle to bring Vanadium into their projects. For example, in
an Android project, the user may do something like:

```groovy
// MyAndroidProject/app/build.gradle

apply plugin: 'com.android.application'

// ...

repositories {
    jCenter()
}

dependencies {
    compile 'io.v:vanadium-android:1.6'
}
```

In this example, using Gradle to build the application will cause a binary JAR
version of Vanadium for Android to be downloaded from a Maven repository on the
Internet (in this case, the JCenter repository). While this is very convenient
for the end user, it is not so convenient to the Vanadium contributor who wishes
to test some changes without making a full release.

To get around this, we make use of the fact that Maven can be configured to use
a repository on the local filesystem. Gradle gives us convenient access to this
repository under the `mavenLocal` moniker. In order to publish to the local
Maven repository, you will:

1. change `lib/build.gradle`, set `releaseVersion` to something that is not in
   use (e.g. '9.9')
2. in the Java root directory, run `./gradlew :lib:clean
   :lib:publishToMavenLocal`
3. if you wish to test changes to the Vanadium for Android library, repeat the
   above steps for the `android-lib` project (i.e. change
   `android-lib/build.gradle` and run `./gradlew :android-lib:clean
   :android-lib:publishToMavenLocal`). Ensure that the `releaseVersion`
   variables in the two `build.gradle` files are the same.

Now that the library is published, you can modify the example Android app's
`build.gradle` as follows:

```groovy
// MyAndroidProject/app/build.gradle

apply plugin: 'com.android.application'

// ...

repositories {
    mavenLocal()  // <-- This must come first, repositories are searched
                  // in the order they appear in this file.
    jCenter()
}

dependencies {
    compile 'io.v:vanadium-android:9.9'  // Set this to the releaseVersion
                                         // you chose above.
}
```

Importantly, you can re-run step 2 above as many times as you like without
having to choose a new `releaseVersion`.

