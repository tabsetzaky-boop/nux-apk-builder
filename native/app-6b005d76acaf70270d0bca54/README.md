# متجر نوكس

تطبيق أندرويد أصلي (Kotlin + Jetpack Compose) وُلّد من منصة Nux.

- معرّف الحزمة: `com.nux.app`
- الإصدار: 1.0.0 (1)
- الشاشات: الرئيسية · السلة والمنتج · طلباتي · تسجيل الدخول · لوحة الإدارة
- الأذونات: NOTIFICATIONS

## البناء

```bash
export NUX_KEYSTORE=/path/to/release.jks
export NUX_STORE_PASS=...
export NUX_KEY_ALIAS=nux
export NUX_KEY_PASS=...
gradle assembleRelease
```

الملف الناتج: `app/build/outputs/apk/release/app-release.apk`.

المخطط في `app/src/main/assets/spec.json` هو نفسه المستخدم في معاينة المنصة.
