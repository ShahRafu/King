package com.shahrafuking.kingassistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.widget.Toast

/**
 * SettingsDrawerContent - contains the final settings drawer items described by the owner.
 * Each item currently opens a placeholder action (Toast) or navigates to a screen in the branch.
 */

@Composable
fun SettingsDrawerContent(ctx: Context = LocalContext.current, onClose: () -> Unit = {}) {
    Column(modifier = Modifier
        .fillMaxHeight()
        .padding(12.dp)) {
        Text(text = "King Assistant Settings", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(12.dp))

        DrawerItem("1. নতুন ফাইল ও প্লাগইন স্ট্যাটাস") { Toast.makeText(ctx, "Open plugins status (placeholder)", Toast.LENGTH_SHORT).show() }
        DrawerItem("2. স্থায়ী স্মৃতি ও সাপ্তাহিক আর্কাইভ") { Toast.makeText(ctx, "Open backups/archives (placeholder)", Toast.LENGTH_SHORT).show() }
        DrawerItem("3. মার্কেটিং ও ট্রেড মেমোরি নোটপ্যাড") { Toast.makeText(ctx, "Open trade notepad (placeholder)", Toast.LENGTH_SHORT).show() }
        DrawerItem("4. ব্যক্তিগত অল-মিডিয়া নোটপ্যাড") { Toast.makeText(ctx, "Open personal media (placeholder)", Toast.LENGTH_SHORT).show() }
        DrawerItem("5. অ্যাপ লোগো ও ব্র্যান্ড থিম") { Toast.makeText(ctx, "Open theme/logo (placeholder)", Toast.LENGTH_SHORT).show() }
        DrawerItem("6. ভয়েস-প্রিন্ট ও বায়োমেট্রিক ক্যালিব্রেশন") { Toast.makeText(ctx, "Open voice enrollment (placeholder)", Toast.LENGTH_SHORT).show() }
        DrawerItem("7. এপিআই কি ও সিক্রেট কি ম্যানেজার") { Toast.makeText(ctx, "Open API keys (placeholder)", Toast.LENGTH_SHORT).show() }
        DrawerItem("8. নেটওয়ার্ক ও আইপি রোটেশন কন্ট্রোল") { Toast.makeText(ctx, "Open network control (placeholder)", Toast.LENGTH_SHORT).show() }
        DrawerItem("9. পারমিশন ও সিস্টেম হেলথ কন্ট্রোলার") { Toast.makeText(ctx, "Open permissions (placeholder)", Toast.LENGTH_SHORT).show() }
        DrawerItem("10. জরুরি ডেটা রিকোভারি ও ক্লাউড সিঙ্ক") { Toast.makeText(ctx, "Open recovery/cloud sync (placeholder)", Toast.LENGTH_SHORT).show() }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
            Text("Close")
        }
    }
}

@Composable
private fun DrawerItem(title: String, onClick: () -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp), elevation = 2.dp, shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier
            .clickable(onClick = onClick)
            .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 14.sp)
        }
    }
}
