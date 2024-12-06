# lanternsdk-android
This SDK enables the user to embed Lantern in order to provide censorship circumvention to any
network APIs that respect the [default ProxySelector](https://developer.android.com/reference/java/net/ProxySelector#getDefault()).

## Output
After running the build, you can find the library at `./sdk/libs/lanternsdk-android.aar`. This
library embeds the Go library, so consumers of the SDK just need lanternsdk-android.aar and nothing
else.

## Usage

### Setting up Lantern
To setup your app to integrate Lantern, you should first specify some initial configuration for the
SDK to use, including the app name and configuration directory.

```kotlin
import android.content.Context
import io.lantern.sdk.Lantern

Lantern.setup("HelloVPN", "HelloVPN/config")
```

### Starting Lantern

After starting Lantern, it will be set as the system. It blocks up til the given timeout and returns the address the proxy is listening. If the proxy doesn't start within the given timeout, this method returns an error.

After starting Lantern, all HTTP traffic will be proxied.

```kotlin
import android.content.Context
import io.lantern.sdk.Lantern

...
val context: Context = ...
val appName = "your app name assigned by Lantern"
val proxyAddr = ":8080"
val startTimeoutMillis = 60000L // 60 seconds
val proxyAllTraffic = true
Lantern.start(context, appName, proxyAddr, proxyAllTraffic, startTimeoutMillis)
```

### Stopping Lantern
After stopping Lantern, Lantern will continue to run in the background to keep fetching updated
configuration maintain its state, but no traffic will be proxied.

```kotlin
Lantern.stop()
```

### Restarting Lantern Again
Lantern can be restarted again after stopping it. This will be a fast start since Lantern is already
running

```kotlin
Lantern.restart(context, startTimeoutMillis)
```
