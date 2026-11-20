package com.cinntra.ledurecare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.cinntra.ledurecare.navigation.AppNavHost
import com.cinntra.ledurecare.ui.theme.LedureCareTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LedureCareTheme {
                // Screens own their insets so the login video can run edge to edge.
                AppNavHost(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
