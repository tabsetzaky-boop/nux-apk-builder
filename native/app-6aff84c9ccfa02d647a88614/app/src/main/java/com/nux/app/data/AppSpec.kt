package com.nux.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** مخطط التطبيق المحمّل من assets/spec.json — نفس المخطط الذي تُبنى منه المعاينة. */
class AppSpec(val root: JSONObject) {

    val appName: String
        get() {
            val value = root.optJSONObject("app")?.optString("name") ?: ""
            return if (value.isBlank()) "التطبيق" else value
        }

    val theme: JSONObject
        get() = root.optJSONObject("theme") ?: JSONObject()

    val primary: String
        get() = theme.optString("primary", "#0A84FF")

    val dark: Boolean
        get() = theme.optBoolean("dark", false)

    val radius: Int
        get() = theme.optInt("radius", 20)

    val startId: String
        get() {
            val declared = root.optString("start")
            if (declared.isNotBlank()) return declared
            return screens().firstOrNull()?.optString("id") ?: "home"
        }

    fun screens(): List<JSONObject> = toList(root.optJSONArray("screens"))

    fun navScreens(): List<JSONObject> {
        val ids = toIds(root.optJSONArray("bottomNav"))
        return screens().filter { ids.contains(it.optString("id")) }
    }

    fun dataModel(name: String): JSONObject? =
        toList(root.optJSONArray("data")).firstOrNull { it.optString("name") == name }

    /** الأذونات الخطرة التي تُطلب من المستخدم وقت التشغيل فقط عند الحاجة. */
    fun runtimePermissions(): List<String> {
        val out = ArrayList<String>()
        for (id in toIds(root.optJSONArray("permissions"))) {
            when (id) {
                "CAMERA" -> out.add(android.Manifest.permission.CAMERA)
                "MICROPHONE" -> out.add(android.Manifest.permission.RECORD_AUDIO)
                "LOCATION" -> {
                    out.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    out.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                }
                "STORAGE" -> if (android.os.Build.VERSION.SDK_INT >= 33) {
                    out.add(android.Manifest.permission.READ_MEDIA_IMAGES)
                }
                "NOTIFICATIONS" -> if (android.os.Build.VERSION.SDK_INT >= 33) {
                    out.add(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                "CONTACTS" -> out.add(android.Manifest.permission.READ_CONTACTS)
                "CALENDAR" -> out.add(android.Manifest.permission.READ_CALENDAR)
            }
        }
        return out
    }

    companion object {
        fun load(context: Context): AppSpec {
            val text = context.assets.open("spec.json").bufferedReader().use { it.readText() }
            return AppSpec(JSONObject(text))
        }

        fun toList(array: JSONArray?): List<JSONObject> {
            if (array == null) return emptyList()
            val list = ArrayList<JSONObject>()
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index)
                if (item != null) list.add(item)
            }
            return list
        }

        fun toIds(array: JSONArray?): List<String> {
            if (array == null) return emptyList()
            val list = ArrayList<String>()
            for (index in 0 until array.length()) list.add(array.optString(index))
            return list
        }
    }
}
