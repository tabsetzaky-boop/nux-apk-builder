package com.nux.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.json.JSONArray
import org.json.JSONObject
import com.nux.app.data.AppSpec
import com.nux.app.data.AppStore

@Composable
fun NuxApp(spec: AppSpec, store: AppStore) {
    val nav = rememberNavController()
    val colors = remember(spec) { NuxColors(spec) }
    val navScreens = remember(spec) { spec.navScreens() }
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: spec.startId

    Scaffold(
        containerColor = colors.background,
        bottomBar = {
            if (navScreens.isNotEmpty()) {
                NavigationBar(containerColor = colors.surface) {
                    for (item in navScreens) {
                        val id = item.optString("id")
                        NavigationBarItem(
                            selected = currentRoute == id,
                            onClick = {
                                if (currentRoute != id) nav.navigate(id) { launchSingleTop = true }
                            },
                            icon = { Text(item.optString("icon", "•"), fontSize = 16.sp) },
                            label = { Text(item.optString("title", id), fontSize = 10.sp) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = spec.startId,
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            for (screen in spec.screens()) {
                composable(screen.optString("id")) {
                    ScreenView(spec, screen, nav, store, colors)
                }
            }
        }
    }
}

@Composable
fun ScreenView(
    spec: AppSpec,
    screen: JSONObject,
    nav: NavHostController,
    store: AppStore,
    colors: NuxColors
) {
    val components = screen.optJSONArray("components") ?: JSONArray()
    val values = remember(screen.optString("id")) { mutableStateMapOf<String, String>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(start = 20.dp, top = 28.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = screen.optString("title"),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text
                )
                val subtitle = screen.optString("subtitle")
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = colors.muted,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
        items(count = components.length()) { index ->
            val component = components.optJSONObject(index)
            if (component != null) {
                ComponentView(component, spec, nav, store, colors, values)
            }
        }
    }
}

@Composable
fun ComponentView(
    component: JSONObject,
    spec: AppSpec,
    nav: NavHostController,
    store: AppStore,
    colors: NuxColors,
    values: SnapshotStateMap<String, String>
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    when (component.optString("type")) {
        "heading" -> Text(
            text = component.optString("text"),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.text
        )
        "text" -> Text(
            text = component.optString("text"),
            fontSize = 14.sp,
            lineHeight = 22.sp,
            color = colors.muted
        )
        "button" -> {
            val primary = component.optBoolean("variant", true)
            Button(
                onClick = { runAction(component.optJSONObject("action"), spec, nav, store, values, context) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(spec.radius.dp),
                colors = if (primary) {
                    ButtonDefaults.buttonColors(containerColor = colors.primary)
                } else {
                    ButtonDefaults.buttonColors(containerColor = colors.surface, contentColor = colors.text)
                }
            ) {
                Text(component.optString("label", "متابعة"), fontSize = 14.sp)
            }
        }
        "input" -> FieldInput(component, colors, values)
        "form" -> {
            val fields = component.optJSONArray("fields") ?: JSONArray()
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (index in 0 until fields.length()) {
                    val field = fields.optJSONObject(index)
                    if (field != null) FieldInput(field, colors, values)
                }
                Spacer(Modifier.height(2.dp))
                Button(
                    onClick = { runAction(component.optJSONObject("action"), spec, nav, store, values, context) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(spec.radius.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text(component.optString("submitLabel", "حفظ"), fontSize = 14.sp)
                }
            }
        }
        "list" -> ListBlock(component, spec, store, colors)
        "card" -> CardBlock(component, spec, nav, store, colors, values, context)
        "chips" -> ChipsRow(component, colors)
        "toggle" -> ToggleRow(component, store, colors)
        "stat" -> StatBlock(component, store, colors)
        "image" -> ImageBlock(component, colors)
        "banner" -> Surface(
            color = colors.primary.copy(alpha = 0.12f),
            shape = RoundedCornerShape(spec.radius.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = component.optString("text"),
                fontSize = 13.sp,
                color = colors.text,
                modifier = Modifier.padding(14.dp)
            )
        }
        "divider" -> HorizontalDivider(color = colors.muted.copy(alpha = 0.3f))
        else -> {
            val text = component.optString("text")
            if (text.isNotBlank()) Text(text = text, fontSize = 14.sp, color = colors.muted)
        }
    }
}

@Composable
private fun FieldInput(field: JSONObject, colors: NuxColors, values: SnapshotStateMap<String, String>) {
    val key = field.optString("field")
    Column {
        val label = field.optString("label")
        if (label.isNotBlank()) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = colors.muted,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        OutlinedTextField(
            value = values[key] ?: "",
            onValueChange = { values[key] = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            placeholder = {
                Text(field.optString("placeholder"), fontSize = 13.sp, color = colors.muted)
            }
        )
    }
}

@Composable
private fun ListBlock(component: JSONObject, spec: AppSpec, store: AppStore, colors: NuxColors) {
    val revision = store.revision
    val source = component.optString("source")
    val rows = if (source.isNotBlank()) store.items(source) else emptyList()

    if (rows.isEmpty()) {
        Surface(
            color = colors.surface,
            shape = RoundedCornerShape(spec.radius.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = component.optString("empty", "لا توجد عناصر بعد"),
                fontSize = 13.sp,
                color = colors.muted,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in rows) {
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(spec.radius.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = row.optString(component.optString("titleField", "title")),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.text
                        )
                        val subtitle = row.optString(component.optString("subtitleField", "subtitle"))
                        if (subtitle.isNotBlank()) {
                            Text(text = subtitle, fontSize = 12.sp, color = colors.muted)
                        }
                    }
                    if (component.optBoolean("removable", true) && revision >= 0) {
                        TextButton(onClick = { store.remove(source, row.optString("id")) }) {
                            Text("حذف", fontSize = 12.sp, color = colors.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardBlock(
    component: JSONObject,
    spec: AppSpec,
    nav: NavHostController,
    store: AppStore,
    colors: NuxColors,
    values: SnapshotStateMap<String, String>,
    context: Context
) {
    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(spec.radius.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val title = component.optString("title")
            if (title.isNotBlank()) {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.text)
            }
            val body = component.optString("text")
            if (body.isNotBlank()) {
                Text(
                    text = body,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = colors.muted,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            val action = component.optJSONObject("action")
            if (action != null && action.optString("type") != "none") {
                TextButton(
                    onClick = { runAction(action, spec, nav, store, values, context) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("فتح", fontSize = 13.sp, color = colors.primary)
                }
            }
        }
    }
}

@Composable
private fun ChipsRow(component: JSONObject, colors: NuxColors) {
    val items = component.optJSONArray("items") ?: JSONArray()
    if (items.length() == 0) return
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (index in 0 until items.length()) {
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(end = 2.dp)
            ) {
                Text(
                    text = items.optString(index),
                    fontSize = 12.sp,
                    color = colors.text,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(component: JSONObject, store: AppStore, colors: NuxColors) {
    val revision = store.revision
    val key = component.optString("field")
    val checked = if (revision >= 0) store.flag(key) else false
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = component.optString("label"),
            fontSize = 14.sp,
            color = colors.text,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = { store.setFlag(key, it) },
            colors = SwitchDefaults.colors(checkedThumbColor = colors.primary)
        )
    }
}

@Composable
private fun StatBlock(component: JSONObject, store: AppStore, colors: NuxColors) {
    val revision = store.revision
    val source = component.optString("source")
    val value = if (revision >= 0 && source.isNotBlank()) {
        store.items(source).size.toString()
    } else {
        component.optString("value", "—")
    }
    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.width(150.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = component.optString("label"), fontSize = 12.sp, color = colors.muted)
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colors.text,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ImageBlock(component: JSONObject, colors: NuxColors) {
    val height = component.optInt("height", 140).coerceIn(80, 220)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp)
            .background(colors.surface, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = component.optString("emoji", "🖼️"), fontSize = 34.sp)
            val caption = component.optString("caption")
            if (caption.isNotBlank()) {
                Text(
                    text = caption,
                    fontSize = 12.sp,
                    color = colors.muted,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

fun runAction(
    action: JSONObject?,
    spec: AppSpec,
    nav: NavHostController,
    store: AppStore,
    values: Map<String, String>,
    context: Context
) {
    if (action == null) return
    when (action.optString("type")) {
        "navigate" -> {
            val target = action.optString("target")
            if (target.isNotBlank()) nav.navigate(target) { launchSingleTop = true }
        }
        "add" -> {
            val data = action.optString("data")
            val model = spec.dataModel(data) ?: return
            val fields = model.optJSONArray("fields") ?: JSONArray()
            val record = JSONObject()
            for (index in 0 until fields.length()) {
                val field = fields.optJSONObject(index) ?: continue
                val name = field.optString("name")
                if (name.isBlank()) continue
                val value = values[name] ?: ""
                if (value.isNotBlank()) record.put(name, value)
            }
            store.add(data, record)
        }
        "save" -> {
            val data = action.optString("data")
            val record = JSONObject()
            for ((key, value) in values) record.put(key, value)
            store.putValue(data, record.toString())
        }
        "clear" -> store.clear(action.optString("data"))
        "external" -> {
            val url = action.optString("url")
            if (url.isNotBlank()) {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (error: Exception) {
                    // لا يوجد تطبيق قادر على فتح الرابط.
                }
            }
        }
    }
}
