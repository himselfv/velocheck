Показывает список парковок Velobike и свободные велосипеды на каждой. Можно выбирать любимые.

Использованы иконки:
https://www.iconfinder.com/iconsets/small-n-flat

API Key для Maps и его связь с debug/release signing:
 1. на каждом компе разработки отладочный ключ свой
 2. нужно хоть раз собрать debug, чтобы он появился
 3. из него с помощью keytool получить sha1
 4. и добавить такой restriction к своему API ключу.
Когда распространяешь в форме APK, то там уже подписано чем подписано.
https://developers.google.com/maps/documentation/android-sdk/get-api-key

App signing (для релиза):
https://developer.android.com/studio/publish/app-signing#generate-key
