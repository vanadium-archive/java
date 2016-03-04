The Baku Android Toolkit includes software components facilitating the
development of applications with distributed user interfaces.

## Getting Started
To get started, see the {@link io.v.baku.toolkit} package docs.

## Usage
The Baku Android Toolkit library is available from JCenter and Maven
Central. The available versions are listed at
https://bintray.com/vanadium/io.v/baku-toolkit. To use the Baku Toolkit
from an Android Java project, ensure that the {@code build.gradle} has
either `jcenter()` or `mavenCentral()` in its repositories, add
`'io.v:baku-toolkit:version'` as a `compile` dependency, and bind an
[SLF4J](http://www.slf4j.org/) logger as an APK dependency, like
`apk ('org.slf4j:slf4j-android:1.7.12')`.