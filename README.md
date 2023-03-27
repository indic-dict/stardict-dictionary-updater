Android app to download and install [a large set of Sanskrit (and other) dictionaries](https://github.com/indic-dict), which can then be used from any compatible dictionary app.

## For app users
- Play store link: <https://play.google.com/store/apps/details?id=sanskritcode.sanskritdictionaryupdater>
- Amazon store link: <https://amazon.com/dp/B07HBPZ7P6>
- [Ratings Certificate](https://iarcweb.azurewebsites.net/Dashboard/Certificate/74e40614-671c-421e-9969-1c80da21a267)
- Signed apk is also [released (https://rebrand.ly/dict-updater)](https://github.com/indic-dict/stardict-dictionary-updater/raw/master/dictUpdaterApp/release/dictUpdaterApp-release.aab) in this repository.

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


## Store listings
Publish to Google Play [here](https://play.google.com/console/u/0/developers/9181945829356368365/app/4975588785652561253/tracks/4697271960125342543?tab=releases).

### hi-IN
```
प्राप्यान्तर्जालात् स्टार्डिक्ट-कोशानां सूचिः, स्थापयति तान् कोशान् यन्त्रे SDCARD/dictdata इत्यत्र। एवं स्थापिताः सञ्चिकाः stardict-प्रयोक्तृ-तन्त्रांशैः क्रियतामिति। स्टार्डिक्ट्-प्रयोक्तृ-तान्त्रांशः कश्चित् (उदाहरणार्थम् - ebdic (प्रशस्तम्), ColorDict, GoldenDict, GoldenDict paid ...) युष्माभिः स्थापितो वा स्थाप्यते वेति नः प्रत्ययः।

वयं stardict-sanskrit-गणेन प्रकाशिताः कोशसूचीः प्रयुञ्ज्महे। (ईक्षन्ताम् : https://github.com/indic-dict/stardict-sanskrit , https://sanskrit-coders.github.io/ )।  लघुसङ्केतः  पृष्ठस्यास्य- bit.ly/SanskritStarDictUpdater । संप्रत्य् एतासु भाषासु ११३+ कोषास् सङ्गृहीता अस्माभिः - Sanskrit, pALi, hindI, marathi, punjabi/ panjabi, nepali, oriya/ odiya, assamese/ Asamiya, kannaDa, telugu, tamiL / tamizh, malayalam, sinhala/ sinhalese, greek, latin, english (संस्कृतम्, पाली/ पाळी, हिन्दी, ਪੰਜਾਬੀ/ پنجابی‬, ଓଡ଼ିଆ,  অসমীয়া, ಕನ್ನಡ,  සිංහල, తెలుగు, தமிழ், മലയാളം, ελληνικά, lingua latīna)  येषां स्वलिपिमन्तरा देवनागर्या रोमकलिप्या चाप्य् अन्वेषणं शक्यम्।

क्लेशान् अत्र सूचयत, परिहरत, पश्यत च - https://github.com/indic-dict/stardict-dictionary-updater/issues ।

Report / solve issues, look at known problems here: https://github.com/sanskrit-coders/indic-dict/issues . PS: If this app does not work on your old phone for some reason, consider alternatives presented here - https://sanskrit-coders.github.io/ (but do report the failure).

Privacy Policy: https://github.com/indic-dict/stardict-dictionary-updater/blob/master/privacy-policy.md

जय हनुमान्। श्रीरामो जयतितमाम्।
```

### English
```
This program downloads latest dictionary files from the internet, and stores it in the SDCARD/dictdata folder, for Stardict apps to use. We assume that you already have or will install Stardict-compatible dictionary utilities (Eg: ebdic [Rated high], ColorDict, GoldenDict, GoldenDict paid ...) separately.

We use the indices maintained by the stardict-sanskrit project (See: https://github.com/indic-dict/stardict-sanskrit , https://sanskrit-coders.github.io/ ). [If you want to contribute new dictionaries to the stardict-sanskrit project or code to this app, that is welcome too - visit sites above.] Short-link to this app: bit.ly/SanskritStarDictUpdater

Note: Our indices allow for 113+ dictionaries in the following languages to be installed: Sanskrit, pALi, hindI, marathi, punjabi/ panjabi, nepali, oriya/ odiya, assamese/ Asamiya, kannaDa, telugu, tamiL / tamizh, malayalam, sinhala/ sinhalese, greek, latin, english (संस्कृतम्, पाली/ पाळी, हिन्दी, ਪੰਜਾਬੀ/ پنجابی‬, ଓଡ଼ିଆ,  অসমীয়া, ಕನ್ನಡ,  සිංහල, తెలుగు, தமிழ், മലയാളം, ελληνικά, lingua latīna) which you can search with the native script as well as with intuitive devanAgarI and roman transliterations.

Report / solve issues, look at known problems here: https://github.com/indic-dict/stardict-dictionary-updater/issues . PS: If this app does not work on your old phone for some reason, consider alternatives presented here - https://sanskrit-coders.github.io/ (but do report the failure).

Dicts available as of 201704-
<pre>
stardict-hindi/hi-head/                            : 1
stardict-kannada/en-head/                          : 1
stardict-kannada/kn-head/                          : 2
stardict-pali/en-head/                             : 2
stardict-pali/pali-en-head/                        : 2
stardict-pali/pali-head/                           : 2
stardict-sanskrit/en-head/                         : 5
stardict-sanskrit/sa-head/en-entries/              : 18
stardict-sanskrit/sa-head/other-entries/           : 5
stardict-sanskrit/sa-head/other-indic-entries/     : 1
stardict-sanskrit/sa-head/sa-entries/              : 3
stardict-sanskrit/sa-kAvya/                        : 10
stardict-sanskrit/sa-vyAkaraNa/                    : 26
stardict-tamil/ta-head/                            : 2
stardict-telugu/en-head/                           : 1
stardict-telugu/te-head/                           : 3
Total                                              : 84
</pre>

Privacy Policy: https://github.com/indic-dict/stardict-dictionary-updater/blob/master/privacy-policy.md

जय हनुमान्। श्रीरामो जयतितमाम्।
```