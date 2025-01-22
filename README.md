# Lantern SDK for Android
The Lantern SDK provides a way for developers and third parties to integrate Lantern and access its infrastructure for censorship circumvention. The SDK is designed to work with network APIs that respect the default ProxySelector. It integrates with Lantern’s core proxying functionality and offers a Kotlin-friendly API for easy integration.

## Overview

- [lantern](lantern): The core Go logic for configuring and launching Lantern (via Flashlight)
- [sdk](sdk): The SDK module that wraps the Go functions in a Kotlin-friendly interface
- [example](example): Demonstrates how to integrate the Lantern SDK in an actual Android application

## Getting Started

### Build the Lantern SDK

The following command builds both the Lantern core library and the Android SDK:

```bash
make build-sdk
```

This will produce the SDK as an .aar file located at `./build/lanternsdk-android.aar`.

The .aar file contains the compiled Go library and Kotlin bindings, ready for integration into your Android app.

### Integrating the Lantern SDK

1. Add the SDK to Your Project

Copy the lanternsdk-android.aar file to your app’s libs/ directory and update your app’s build.gradle:

```groovy
repositories {
    flatDir {
        dirs 'libs'
    }
}

dependencies {
    implementation(name: 'lanternsdk-android', ext: 'aar')
}
```

2. Initialize Lantern

Before starting the Lantern proxy, initialize the SDK with your app name and configuration directory:

```kotlin
import io.lantern.sdk.LanternManager

LanternManager.setup(context, "HelloVPN", "HelloVPN/config")
```

3. Start the Lantern Proxy

Start Lantern with the desired proxy configuration:

```kotlin
import io.lantern.sdk.LanternManager


val context: Context = // ...
val proxyAddr = ":8080"  // Proxy address (port 8080 in this example
val proxyAllTraffic = true // Set to true to proxy all traffic

LanternManager.startLantern(context, proxyAddr, proxyAllTraffic)
```

After starting Lantern, it will be set as the system proxy and all HTTP traffic will be proxied.
This method returns the address the proxy is listening. If the proxy fails to start, it returns an error.

4. Stop the Lantern Proxy

To stop actively proxying traffic:

```kotlin
LanternManager.stopLantern()
```

Lantern will continue running in the background to update its configuration but will no longer proxy traffic.
