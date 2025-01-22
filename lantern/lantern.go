// package lantern provides an API for interacting with flashlight
package lantern

// #cgo LDFLAGS: -static-libstdc++
import "C"

import (
	"errors"
	"fmt"
	"net"
	"strconv"
	"sync"
	"time"

	"github.com/getlantern/flashlight/v7"
	"github.com/getlantern/flashlight/v7/client"
	"github.com/getlantern/flashlight/v7/stats"
	"github.com/getlantern/golog"
	"github.com/getlantern/lantern-client/internalsdk/common"
)

const defaultStartTimeout = 5 * time.Second

var (
	log           = golog.LoggerFor("lantern")
	lanternClient *LanternClient
)

// LanternClient is the exported type used by gomobile for Lantern operations.
type LanternClient struct {
	client    *client.Client
	configDir string
	appName   string
	once      sync.Once
	mu        sync.Mutex

	started bool
}

// NewLanternClient initializes a new Lantern client instance.
func NewLanternClient() *LanternClient {
	return &LanternClient{}
}

// StartResult provides information about the started Lantern
type StartResult struct {
	Addr string
}

// Setup is used to initially configure the Lantern SDK and specifies the config directory and app name to use
// - appName: unique identifier for the current application (used for assigning proxies and tracking usage)
// - configDir: application directory to place Lantern configure files
func (lc *LanternClient) Setup(appName, configDir string) {
	fmt.Printf("Lantern setup with appName=%s, configDir=%s\n", appName, configDir)
	lc.mu.Lock()
	lc.configDir = configDir
	lc.appName = appName
	lc.mu.Unlock()
}

// Start blocks up to the given timeout while launching Lantern.
func (lc *LanternClient) Start(httpProxyAddr string, proxyAll bool) (*StartResult, error) {
	if lc.started {
		return nil, errors.New("Lantern is already running")
	}
	timeout := defaultStartTimeout

	lc.once.Do(func() {
		go lc.runLantern(httpProxyAddr, proxyAll)
	})

	addr, ok := client.Addr(timeout)
	if !ok {
		return nil, fmt.Errorf("HTTP Proxy didn't start within %v timeout", timeout)
	}
	lc.started = true
	return &StartResult{addr.(string)}, nil
}

// Stops circumventing with Lantern. Lantern will actually continue running in the background
// in order to keep its configuration up-to-date. Subsequent calls to start() will reuse the
// running Lantern and complete quickly.
func (lc *LanternClient) Stop() error {
	if !lc.started {
		return errors.New("Lantern is not running")
	}
	if err := lc.client.Stop(); err != nil {
		return err
	}
	lc.started = false
	return nil
}

// HTTPProxyPort returns the port the HTTP proxy is listening on
func (lc *LanternClient) HTTPProxyPort() (int, error) {
	result, isValid := client.Addr(5 * time.Second)
	if !isValid {
		return 0, errors.New("flashlight is not running")
	}
	_, portStr, _ := net.SplitHostPort(result.(string))
	port, _ := strconv.Atoi(portStr)
	return port, nil
}

func (lc *LanternClient) runLantern(httpProxyAddr string, proxyAll bool) *flashlight.Flashlight {
	appName, configDir := lc.appName, lc.configDir
	log.Debugf("Starting lantern: configDir %s", configDir)

	userConfig := common.NewUserConfig("", "a34113", 3456344, "tok123", map[string]string{}, "")

	runner, err := flashlight.New(
		appName,
		common.ApplicationVersion,
		common.RevisionDate,
		configDir,                    // place to store lantern configuration
		false,                        // don't enable vpn mode for Android (VPN is handled in Java layer)
		func() bool { return false }, // always connected
		func() bool { return proxyAll },
		func() bool { return false }, // do not proxy private hosts on Android
		func() bool { return true },  // auto report
		map[string]interface{}{},
		userConfig,
		stats.NewTracker(),
		func() bool { return false },
		func() string { return "" }, // only used for desktop
		nil,
		func(category, action, label string) {},
	)
	if err != nil {
		log.Fatalf("failed to start flashlight: %v", err)
	}
	go func() {
		runner.Run(
			httpProxyAddr, // listen for HTTP on provided address
			"127.0.0.1:0", // listen for SOCKS on random address
			func(c *client.Client) {
				lc.client = c
			},
			nil, // onError
		)
	}()
	return runner
}
