package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MikrotechViewModel
import com.example.ui.screens.MainAppScaffold
import com.example.ui.theme.MikrotechTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MikrotechViewModel = viewModel()
            val isLightMode by viewModel.isLightMode.collectAsState()
            MikrotechTheme(darkTheme = !isLightMode) {
                MainAppScaffold(viewModel = viewModel)
            }
        }
    }
}
