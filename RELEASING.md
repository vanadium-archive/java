Releasing Java components
-------------------------

# Introduction

This document describes the process for releasing Vanadium Java components to
JCenter and Maven Central. The components covered by this document are the

* [core Vanadium Java] libraries
* [Vanadium Android] libraries
* [Gradle plugin]

## Background

JCenter and Maven Central are two large repositories of Java and other software
binaries. Tools such as Maven and Gradle can fetch binaries from these
repositories. Many projects use these tools to manage their builds. By providing
Vanadium Java and Android libraries through these channels, the Vanadium team is
making it easy for developers using these tools to get Vanadium into software
projects.

# Release process

For the purposes of the release process, we consider the Java and Android
libraries to be one component. They should be released at the same time. The
Gradle plugin can be released separately.

## Prerequisites

The following one-time steps should be done before making your first release.

### Bintray account

Release engineers should have a Bintray account, which will be used to push
binaries to JCenter and Maven Central. To create a Bintray account:

* go to the [Bintray home page]
* click the "Sign in" button
* click on the "GitHub" button. If necessary, you will be prompted to sign into
  GitHub. You will then need to grant Bintray access to your GitHub account

The account is now created. You need to be part of the Vanadium Bintray
organization. To do this:

* visit the [Vanadium Bintray organization]'s page
* click the "Join" button
* click the "Send" button to send a message to the organization's owners

A member of the Vanadium organization will approve the request.

Once your request has been approved, you need to add your API key to the Gradle
configuration.

* visit https://bintray.com/profile/edit (signing in if necessary)
* generate an API key, copy it to $HOME/.gradle/gradle.properties. The file
  should look like

```
bintrayUsername=yourGithubUsername
bintrayApiKey=yourApiKey
```

### Maven Central account

You need to create a [Sonatype JIRA] account. To do this:

* visit the [JIRA account creation] page and create an account
* send an email to the [vanadium-discuss mailing list]. Your email should be
  something like:

```
To: vanadium-discuss@v.io
Subject: Add Maven Central releaser

Hi there,

I'd like to be added to list of Maven Central releasers. My JIRA username is

<your JIRA username>

Thanks,
New releaser
```

## Gradle plugin

The steps to release the [Gradle plugin] are:

* determine the release number to use. To determine the current release number,
  go to the [JCenter Gradle plugin] page. If this is a major release, increment the
  major release number and set the minor number to zero. If it's a minor
  release, increment the minor number and leave the major number unchanged
* edit `$JIRI_ROOT/release/java/gradle-plugin/build.gradle` and change the
  `releaseVersion` variable to the new release number
* run the following commands:

```sh
cd $JIRI_ROOT/release/java/gradle-plugin
./gradlew clean bintrayUpload
```

This command will build and upload the VDL plugin to Bintray.

* go to the [Gradle plugin Bintray] page and make sure you're signed in
* click on the version number of the version you just uploaded
* you should see a notice such as "You have 4 unpublished item(s) for this
  version", click on the associated "Publish" button

The new version is now published to JCenter. For Maven Central, follow these
additional steps:

* click on the "Maven Central" link
* enter your JIRA username and password that you created in the [Maven
  Central account](#maven-central-account) prerequisite step
* ensure the "Close and release repository when done" check box is checked
* click the "Sync" button

Assuming there were no errors, the new version will be pushed out to Maven
Central shortly (usually within a matter of 10 minutes or so). You can verify by
visiting the [Maven Central page] for the plugin. When the new version number
appears in the list, the plugin has been published.

## Java and Android libraries

Releasing these libraries is similar to releasing the Gradle plugin with one
notable exception: it must be performed once on Linux and once on Darwin. This
is due to the fact that, at this point in time, the Vanadium Java libraries
cannot be cross-compiled between the two platforms.

### Building

On either Linux or Darwin, perform the following steps:

* determine the release number to use. To determine the current release number,
  go to the [JCenter Vanadium library] page. If this is a major release,
  increment the major release number and set the minor number to zero. If it's a
  minor release, increment the minor number and leave the major number unchanged
* edit `$JIRI_ROOT/release/java/lib/build.gradle` and change the
  `releaseVersion` variable to the new release number
* edit `$JIRI_ROOT/release/java/android-lib/build.gradle` and change the
  `releaseVersion` variable to the same release number
* run the following commands:

```sh
cd $JIRI_ROOT/release/java
./gradlew :lib:clean :lib:bintrayUpload
./gradlew :android-lib:clean :android-lib:bintrayUpload
```

Now, switch to the other platform (i.e. not the one you used for the above
steps) and:

* edit the `$JIRI_ROOT/release/java/lib/build.gradle` file
* change the `releaseVersion` variable to match the same variable from the
  previous steps
* find the line that reads

```groovy
    publications = ['mavenJava', 'mavenNoNatives', 'mavenNatives']
```

* we've already published `mavenJava` and `mavenNoNatives`, remove those
  entries. Do not check in your changes. The line should now read

```groovy
    publications = ['mavenNatives']
```

* run the following commands:

```sh
cd $JIRI_ROOT/release/java
./gradlew :lib:clean :lib:bintrayUpload
```

### Publishing

The binaries for the Vanadium and Vanadium Android libraries are now uploaded
to Bintray. Time to publish them to JCenter and Maven Central.

* go to the [Vanadium library Bintray] page and make sure you're signed in
* click on the version number of the version you just uploaded
* you should see a notice such as "You have 8 unpublished item(s) for this
  version", click on the associated "Publish" button

The new Vanadium library version is now published to JCenter. For Maven
Central, follow these additional steps:

* click on the "Maven Central" link
* enter your JIRA username and password that you created in the [Maven
  Central account](#maven-central-account) prerequisite step
* ensure the "Close and release repository when done" check box is checked
* click the "Sync" button

The Vanadium library will now be pushed out to Maven Central in a few minutes.
Now go to the [Vanadium Android library Bintray] page and repeat the publishing
steps.

[core vanadium Java]: https://github.com/vanadium/java/tree/master/lib
[Vanadium Android]: https://github.com/vanadium/java/tree/master/android-lib
[Gradle plugin]: https://github.com/vanadium/java/tree/master/gradle-plugin
[Bintray home page]: https://bintray.com/
[Vanadium Bintray organization]: https://bintray.com/vanadium
[Sonatype JIRA]: https://issues.sonatype.org/
[JIRA account creation]: https://issues.sonatype.org/secure/Signup!default.jspa
[vanadium-discuss mailing list]: mailto:vanadium-discuss@v.io
[JCenter Gradle plugin]: https://jcenter.bintray.com/io/v/gradle-plugin/
[JCenter Vanadium library]: https://jcenter.bintray.com/io/v/vanadium/
[Gradle plugin Bintray]: https://bintray.com/vanadium/io.v/gradle-plugin/view
[Vanadium library Bintray]: https://bintray.com/vanadium/io.v/vanadium/view
[Vanadium Android library Bintray]: https://bintray.com/vanadium/io.v/vanadium-android/view
[Maven Central page]: https://repo1.maven.org/maven2/io/v/gradle-plugin/
