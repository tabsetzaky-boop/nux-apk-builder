package com.nux.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * تخزين محلي حقيقي على الجهاز (SharedPreferences):
 * كل ما يضيفه المستخدم داخل التطبيق يبقى محفوظًا بعد إغلاقه.
 */
class AppStore(context: Context) {

    private val prefs = context.getSharedPreferences("nux_app_data", Context.MODE_PRIVATE)

    var revision by mutableStateOf(0)
        private set

    fun items(name: String): List<JSONObject> {
        if (name.isBlank()) return emptyList()
        val raw = prefs.getString("list_" + name, "[]") ?: "[]"
        val array = try {
            JSONArray(raw)
        } catch (error: Exception) {
            JSONArray()
        }
        val out = ArrayList<JSONObject>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index)
            if (item != null) out.add(item)
        }
        return out
    }

    fun add(name: String, values: JSONObject) {
        if (name.isBlank()) return
        val list = ArrayList(items(name))
        values.put("id", UUID.randomUUID().toString())
        values.put("createdAt", System.currentTimeMillis())
        list.add(values)
        write(name, list)
    }

    fun remove(name: String, id: String) {
        if (name.isBlank() || id.isBlank()) return
        write(name, items(name).filter { it.optString("id") != id })
    }

    fun clear(name: String) {
        if (name.isBlank()) return
        write(name, emptyList())
    }

    private fun write(name: String, list: List<JSONObject>) {
        val array = JSONArray()
        for (item in list) array.put(item)
        prefs.edit().putString("list_" + name, array.toString()).apply()
        revision += 1
    }

    fun putValue(key: String, value: String) {
        if (key.isBlank()) return
        prefs.edit().putString("v_" + key, value).apply()
        revision += 1
    }

    fun value(key: String): String = prefs.getString("v_" + key, "") ?: ""

    fun flag(key: String): Boolean = prefs.getBoolean("f_" + key, false)

    fun setFlag(key: String, value: Boolean) {
        prefs.edit().putBoolean("f_" + key, value).apply()
        revision += 1
    }
}
