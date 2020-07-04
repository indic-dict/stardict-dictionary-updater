Android app to download and install [a large set of Sanskrit (and other) dictionaries](https://github.com/indic-dict), which can then be used from any compatible dictionary app.

## For app users
- Play store link: <https://play.google.com/store/apps/details?id=sanskritcode.sanskritdictionaryupdater>
- Amazon store link: <https://amazon.com/dp/B07HBPZ7P6>
- [Ratings Certificate](https://iarcweb.azurewebsites.net/Dashboard/Certificate/74e40614-671c-421e-9969-1c80da21a267)
- Signed apk is also [released](https://raw.githubusercontent.com/sanskrit-coders/stardict-dictionary-updater/master/app/release/app-release.apk) in this repository.

## For library users
- We publish to a [downloaderFlow bintray package](https://bintray.com/sanskrit-coders/android-repo/downloaderFlow).

### Usage
```
dependencies {
    implementation 'com.github.sanskrit-coders:downloaderFlow:0.0.2@aar'
    // Somehow, dependencies from the above are not automatically deduces (despite downloaderFlow-0.0.2.pom containing them). Hence reincluding them below.
    implementation group: 'com.google.guava', name: 'guava', version: '27.0.1-android'
    implementation('com.loopj.android:android-async-http:1.4.9')
    implementation('org.apache.commons:commons-compress:1.14')
}
repositories {
    ...
    maven {
        url 'https://dl.bintray.com/sanskrit-coders/android-repo'
    }
}
```

## For code contributors
- See comment in MainActivity.kt for a rough understanding of the code.
  - Why do we use java rather than kotlin as source directory? Intellij and android studio don't work optimally otherwise - no autocomplete.
- Review notifications setup: https://support.google.com/googleplay/android-developer/answer/138230?hl=en
- Debugging
  - Parsing adb logcat:
    - Use a good log viewer like `lnav Downloads/logcat.txt`
    - Look for lines with string "****************" to get an idea of when activity lifecycle methods are called.
- Generating dependency tree:
  - `gradle app:dependencies > dep_tree_android.txt`
  - `gradle -q dependencies app:dependencies --configuration compile > dep_tree.txt`

### Publishing to maven
- Build a new version of the aar package by updating libraryVersion in build.gradle and using gradle tasks- assemble.
- Publication location described above.
- Android library publication tips [here](https://medium.com/@yegor_zatsepin/simple-way-to-publish-your-android-library-to-jcenter-d1e145bacf13)
- Relevant gradle tasks: `bintrayUpload`.

### Current problems
- Layout preview reports an error:
  - `The following classes could not be instantiated:
     - android.support.v7.widget.AppCompatTextView`
  - Failed fix attempts:
    - Rebuilding
    - clearing cache
    - invalidating cache and restarting
    - Refresh layout
  - Details [here])(https://stackoverflow.com/questions/29887722/error-rendering-problems-the-following-classes-could-not-be-found-android-suppo/30011016#30011016).

## For dictionary contributors
* Just open an issue in the most appropriate project (stardict-sanskrit, stardict-hindI, stardict-kannada, stardict-pAlI, stardict-tamiL, stardict-telugu), or if there is no match, in this project.

### To contribute new dictionary repositories
* We will just need to list your dictionary repository in <dictioanryIndices.md>. Open an issue in this project.
* Creating your dictionary repository:
  * Just follow the pattern you observe in, say [this repo](<https://raw.githubusercontent.com/indic-dict/stardict-sanskrit/master/sa-head/tars/tars.MD>).
  * Note that the filename of your dictionary should have two parts, separated by __, as in `kRdanta-rUpa-mAlA__2016-02-20_23-22-27`.
    * The first part should be the actual dictionary name, the second the timestamp.
  * All stardict and other dictionary files should have names matching the dictionary name specified above.
