# lanternsdk-android
This SDK enables the user to embed Lantern in order to provide censorship circumvention to any
network APIs that respect the [default ProxySelector](https://developer.android.com/reference/java/net/ProxySelector#getDefault()).

## Output
After running the build, you can find the library at `./build/lanternsdk-android.aar`. This
library embeds the Go library, so consumers of the SDK just need lanternsdk-android.aar and nothing
else.

## Overview

- [lantern](lantern): The core Go logic for configuring and launching Lantern (via Flashlight)
- [sdk](sdk): The SDK module that wraps the Go functions in a Kotlin-friendly interface
- [example](example): Demonstrates how to integrate the Lantern SDK in an actual Android application.

## Usage

### Build Lantern library

To build the compiled Go code for the Lantern library and the SDK, run the following command:

```bash
make build-sdk
```

### Setting up Lantern

To setup your app to integrate Lantern, you should first specify some initial configuration for the
SDK to use, including the app name and configuration directory.

```kotlin
import android.content.Context
import io.lantern.sdk.LanternManager

val context: Context = // ...

LanternManager.setup(context, "HelloVPN", "HelloVPN/config")
```

### Starting Lantern

```kotlin
import android.content.Context
import io.lantern.sdk.LanternManager


val context: Context = // ...
val proxyAddr = ":8080"
val proxyAllTraffic = true

LanternManager.startLantern(context, proxyAddr, proxyAllTraffic)
```

After starting Lantern, it will be set as the system proxy and all HTTP traffic will be proxied.
This method blocks up til the given timeout and returns the address the proxy is listening. If the proxy doesn't start within the given timeout, it returns an error.

### Stopping Lantern

```kotlin
LanternManager.stopLantern()
```

After stopping Lantern, Lantern will continue to run in the background to keep fetching updated
configuration maintain its state, but no traffic will be proxied.

### Restart Lantern
Lantern can be restarted after stopping it. This will be a fast start since Lantern is already
running

```kotlin
LanternManager.restart(context, startTimeoutMillis)
```
