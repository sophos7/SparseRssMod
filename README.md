Sparse Rss Mod 2013-2026
========================
This is an android program to read RSS News.
Originally by Stefan Handschuh, modified by android.makes.fun@gmail.com, now maintained by [sophos7](https://github.com/sophos7/SparseRssMod).

* [Source](https://github.com/sophos7/SparseRssMod)
* [Documentation](doc/doc.md)
* [Sample RSS Feeds](doc/feeds.md)
* [Changelog](doc/changelog.md)


Backup
------
* Menu > Export to OPML / Import from OPML: your feeds
* Menu > Export settings / Import settings: your options (JSON). Starred and read entries are not included.

Privacy
-------
The app sends nothing anywhere by default. Settings > Diagnostics > Send usage and crash data turns on
[Datadog](https://www.datadoghq.com) RUM, crash reporting and session replay. Session replay masks all text, images and
touches unless you turn off "Mask session replay".
To send the data to your own Datadog account instead, enter a client token, RUM application ID and site in the same
section. Those credentials are not part of the settings export.

Building
--------
`./gradlew :sparseRSS:assembleDebug`. Release builds are minified with R8; add `-PlocalRelease` to sign with the debug key.
If `DD_API_KEY` is set, the R8 mapping file is uploaded to Datadog so crash reports are readable.

Thanks
------
* The original developer Stefan Handschuh 
* [HTMLFetcher](https://github.com/karussell/snacktory)
* [Glide](https://github.com/bumptech/glide/blob/master/README.md)
* [TextDrawable](https://github.com/amulyakhare/TextDrawable)
* Google for Android and their Support Libs


Older Versions
--------------
* [2.29](https://apkplz.net/download-app/de.bernd.shandschuh.sparserss) Last Version for Android 5 (2017)
* [2.21](https://apkplz.com/android-apps/sparse-rss-mod/sparse-rss-mod-2-21) Last Version for Android 4
* [1.96](https://apkplz.com/android-apps/sparse-rss-mod/sparse-rss-mod-1-96) Old Version without Support Libs and Cover


Original Readme
---------------
Sparse rss android program

Copyright (c) 2010-2012 Stefan Handschuh

Translators
 - Dutch: Eelko Berkenpies
 - Spanish: Sergio Martín
 - French: <unnamed>
 - Turkish: <unnamed>
 - Russian: Igor Nedoboy
 - Swedish: Lars m

Code-Contributors
 - Joel Low

The file "[LICENSE](LICENSE)" contains the license information for the program.

Icons and artwork are distributed under the CC-BY 3.0 license.


