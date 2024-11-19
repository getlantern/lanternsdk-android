package org.getlantern.lantern.sdk.example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.lantern.sdk.Lantern

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize the SDK
        Lantern.start(this, "Example", true, 0)
        setContentView(R.layout.activity_main)
    }
}
