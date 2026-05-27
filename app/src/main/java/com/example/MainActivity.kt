package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.VaultViewModel

class MainActivity : FragmentActivity() {
    private val vaultViewModel: VaultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0C0E12)
                ) {
                    val isUnlocked by vaultViewModel.isUnlocked.collectAsStateWithLifecycle()

                    AnimatedContent(
                        targetState = isUnlocked,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                        },
                        label = "Vault Authentication Transition"
                    ) { unlocked ->
                        if (unlocked) {
                            DashboardScreen(
                                viewModel = vaultViewModel,
                                onLockClicked = {
                                    vaultViewModel.setUnlocked(false)
                                }
                            )
                        } else {
                            AuthScreen(
                                onAuthSuccess = {
                                    vaultViewModel.setUnlocked(true)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
