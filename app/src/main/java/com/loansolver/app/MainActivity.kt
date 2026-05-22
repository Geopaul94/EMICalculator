package com.loansolver.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.loansolver.app.ui.screen.EMIScreen
import com.loansolver.app.ui.theme.EMICalculatorTheme

// ─────────────────────────────────────────────────────────────────────────────
//  MainActivity — The App Entry Point
//
//  In a Compose app, MainActivity has exactly ONE job:
//    1. Enable edge-to-edge display (draws under status bar / nav bar)
//    2. Set the Compose content (wrap the screen in our theme)
//
//  That's it. No business logic, no UI building, no state.
//  All of that lives in the ViewModel and Screen composables.
//
//  Think of MainActivity as the "front door" — it just opens the door
//  and hands control to the rest of the architecture.
// ─────────────────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // installSplashScreen() MUST be called before super.onCreate() so the
        // OS can display the splash window before Compose starts inflating.
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Makes the app draw behind the system bars (modern full-screen look)
        enableEdgeToEdge()

        setContent {
            // Apply our custom dark theme to everything inside
            EMICalculatorTheme {
                // The one and only screen in this app
                EMIScreen()
            }
        }
    }
}
