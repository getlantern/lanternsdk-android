# lanternsdk-android
This SDK enables the user to embed Lantern in order to provide censorship circumvention to any
network APIs that respect the [default ProxySelector](https://developer.android.com/reference/java/net/ProxySelector#getDefault()).

## Output
After running the build, you can find the library at `./sdk/libs/lanternsdk-android.aar`. This
library embeds the Go library, so consumers of the SDK just need lanternsdk-android.aar and nothing
else.

## Usage

### Starting Lantern
After starting Lantern, all HTTP traffic will be proxied.

```java
import android.content.Context;
import io.lantern.sdk.Lantern;

...

Context context = ...;
String appName = "your app name assigned by Lantern";
long startTimeoutMillis = 60000; // 60 seconds
bool proxyAllTraffic = true;        
Lantern.start(context, appName, proxyAllTraffic, startTimeoutMillis);
```

### Stopping Lantern
After stopping Lantern, Lantern will continue to run in the background to keep fetching updated
configuration maintain its state, but no traffic will be proxied.

...
Lantern.stop();
```

### Starting Lantern Again
Lantern can be started again after stopping it. This will be a fast start since Lantern is already
running.

```
Lantern.start(context, appName, startTimeoutMillis);
```
