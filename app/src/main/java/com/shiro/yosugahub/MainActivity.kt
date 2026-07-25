package com.shiro.yosugahub

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.shiro.yosugahub.ui.YosugaHubApp
import com.shiro.yosugahub.ui.theme.YosugaHubTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    /** 「共有 → Yosuga Hub」で届いた本文。取り込むか捨てるまで保持する。 */
    private val sharedText = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharedText.value = SharedText.from(intent)
        setContent {
            val shared by sharedText.collectAsState()
            YosugaHubTheme {
                YosugaHubApp(
                    sharedText = shared,
                    onSharedTextHandled = { sharedText.value = null },
                )
            }
        }
    }

    /** 起動済みのときの共有(singleTop なのでここへ来る)。 */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        SharedText.from(intent)?.let { sharedText.value = it }
    }
}
