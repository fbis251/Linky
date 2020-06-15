# Linky for Android

### A link/bookmark saving Android application

[![Linky Main Screen](https://raw.githubusercontent.com/fbis251/Linky/static/images/thumbs/00_main.png)](https://raw.githubusercontent.com/fbis251/Linky/static/images/00_main.png)

Linky makes it easy to save your favorite links using any Android application that supports sharing whlie still **respecting your privacy**. The application tries to follow the [Material Design](https://material.google.com/) guidelines to provide a simple, modern interface that follows the Unix philosophy: [Do One Thing and Do It Well](https://en.wikipedia.org/wiki/Unix_philosophy#Do_One_Thing_and_Do_It_Well)

By running and hosting the [Linky Server](https://github.com/fbis251/linky_server) on your own hardware, you will be in full control of your data instead of providing it to a third party.

### Screenshots

[![Login Screen](https://raw.githubusercontent.com/fbis251/Linky/static/images/thumbs/01_login.png)](https://raw.githubusercontent.com/fbis251/Linky/static/images/01_login.png)
[![Fresh Links](https://raw.githubusercontent.com/fbis251/Linky/static/images/thumbs/02_fresh_links.png)](https://raw.githubusercontent.com/fbis251/Linky/static/images/02_fresh_links.png)
[![Navigation Drawer](https://raw.githubusercontent.com/fbis251/Linky/static/images/thumbs/03_drawer.png)](https://raw.githubusercontent.com/fbis251/Linky/static/images/03_drawer.png)
[![All Links](https://raw.githubusercontent.com/fbis251/Linky/static/images/thumbs/04_all_links.png)](https://raw.githubusercontent.com/fbis251/Linky/static/images/04_all_links.png)
[![Favorites](https://raw.githubusercontent.com/fbis251/Linky/static/images/thumbs/05_favorites.png)](https://raw.githubusercontent.com/fbis251/Linky/static/images/05_favorites.png)
[![Archived](https://raw.githubusercontent.com/fbis251/Linky/static/images/thumbs/06_archived.png)](https://raw.githubusercontent.com/fbis251/Linky/static/images/06_archived.png)
[![Sorting Options](https://raw.githubusercontent.com/fbis251/Linky/static/images/thumbs/07_sorting.png)](https://raw.githubusercontent.com/fbis251/Linky/static/images/07_sorting.png)
[![Searching](https://raw.githubusercontent.com/fbis251/Linky/static/images/thumbs/08_searching.png)](https://raw.githubusercontent.com/fbis251/Linky/static/images/08_searching.png)
[![Link Options](https://raw.githubusercontent.com/fbis251/Linky/static/images/thumbs/09_link_options.png)](https://raw.githubusercontent.com/fbis251/Linky/static/images/09_link_options.png)
[![Edit Link](https://raw.githubusercontent.com/fbis251/Linky/static/images/thumbs/10_edit_link.png)](https://raw.githubusercontent.com/fbis251/Linky/static/images/10_edit_link.png)
[![Category AutoComplete](https://raw.githubusercontent.com/fbis251/Linky/static/images/thumbs/11_category_autocomplete.png)](https://raw.githubusercontent.com/fbis251/Linky/static/images/11_category_autocomplete.png)
[![Share](https://raw.githubusercontent.com/fbis251/Linky/static/images/thumbs/12_share.png)](https://raw.githubusercontent.com/fbis251/Linky/static/images/12_share.png)
[![Copy URL](https://raw.githubusercontent.com/fbis251/Linky/static/images/thumbs/13_copy_url.png)](https://raw.githubusercontent.com/fbis251/Linky/static/images/13_copy_url.png)
[![Add Link](https://raw.githubusercontent.com/fbis251/Linky/static/images/thumbs/14_add_link.png)](https://raw.githubusercontent.com/fbis251/Linky/static/images/14_add_link.png)

### Features

- Supports API 15+
- Privacy focused
- Material Design UI
- Works with any application that can share links
- Supports favorites and categories
- Search by URL, title or category

### Building

``` bash
# *nix
./gradlew assembleDebug

# Windows
gradlew assembleDebug
```

If you would like to build a release version of the application, I recommend you import the project into Android Studio and configure the keystore you will use to sign the release version of the application using the built in UI.

If you would like to use an HTTP proxy for debugging, you can edit the [gradle.properties](https://github.com/fbis251/Linky/blob/master/gradle.properties) file and change the `HTTP_PROXY*` options as needed

### Libraries used

- [AppCompat](https://www.android.com/)
- [Design](https://www.android.com/)
- [RecyclerView](https://www.android.com/)
- [BottomSheet](https://github.com/Kennyc1012/BottomSheet)
- [SparkButton](https://github.com/varunest/SparkButton)
- [Gson](https://github.com/google/gson)
- [OkHttp](https://square.github.io/okhttp/)
- [Retrofit](https://square.github.io/retrofit/)
- [realm-java](https://github.com/realm/realm-java)

### License

```
Copyright 2020 Fernando Barillas (FBis251)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
