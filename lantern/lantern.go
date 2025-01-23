// package lantern provides an API for interacting with the flashlight library.
// It is designed to act as a mobile SDK for integrating Lantern's proxying
// and censorship circumvention capabilities.
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
	log                  = golog.LoggerFor("lantern")
	errLanternNotRunning = errors.New("Lantern is not running")
)

// LanternClient provides an interface for configuring and running Lantern's
// HTTP proxy. Each instance manages its own configuration and state
type LanternClient struct {
	client    *client.Client
	configDir string
	appName   string
	mu        sync.Mutex

	started bool
}

// NewLanternClient initializes a new Lantern client instance.
func NewLanternClient() *LanternClient {
	return &LanternClient{}
}

// StartResult provides information about the started Lantern instance,
// including the address where the HTTP proxy is listening.
type StartResult struct {
	Addr string
}

// Setup configures the Lantern SDK by setting the application name and configuration directory.
// This method must be called before starting Lantern.
func (lc *LanternClient) Setup(appName, configDir string) {
	log.Debugf("Lantern setup with appName=%s, configDir=%s", appName, configDir)
	lc.mu.Lock()
	defer lc.mu.Unlock()

	lc.configDir = configDir
	lc.appName = appName
}

// Start initializes and starts the Lantern HTTP proxy. If Lantern is already running,
// this method returns an error. It blocks until the proxy is ready
func (lc *LanternClient) Start(httpProxyAddr string, proxyAll bool) (*StartResult, error) {
	lc.mu.Lock()
	started := lc.started
	lc.mu.Unlock()
	if started {
		return nil, errors.New("Lantern is already running")
	}

	go lc.runLantern(httpProxyAddr, proxyAll)

	timeout := defaultStartTimeout
	addr, ok := client.Addr(timeout)
	if !ok {
		return nil, fmt.Errorf("HTTP Proxy didn't start within %v timeout", timeout)
	}
	// Mark Lantern as started
	lc.mu.Lock()
	lc.started = true
	lc.mu.Unlock()

	return &StartResult{addr.(string)}, nil
}

// Stop disables Lantern's proxy functionality but allows it to continue running
// in the background for configuration updates.
func (lc *LanternClient) Stop() error {
	lc.mu.Lock()
	defer lc.mu.Unlock()

	if !lc.started {
		return errLanternNotRunning
	}

	if lc.client == nil {
		// Lantern hasn't fully initialized the client yet
		return errors.New("client is not fully initialized")
	}

	if err := lc.client.Stop(); err != nil {
		return err
	}
	lc.client = nil
	lc.started = false
	return nil
}

// HTTPProxyPort returns the port the HTTP proxy is listening on
func (lc *LanternClient) HTTPProxyPort() (int, error) {
	lc.mu.Lock()
	defer lc.mu.Unlock()

	if lc.client == nil {
		// Lantern hasn't fully initialized the client yet
		return 0, errors.New("client is not fully initialized")
	}

	result, isValid := lc.client.Addr(5 * time.Second)
	if !isValid {
		return 0, errLanternNotRunning
	}
	_, portStr, _ := net.SplitHostPort(result.(string))
	port, _ := strconv.Atoi(portStr)
	return port, nil
}

// IsRunning returns whether or not Lantern is currently running
func (lc *LanternClient) IsRunning() bool {
	lc.mu.Lock()
	defer lc.mu.Unlock()
	return lc.started
}

// runLantern launches the Lantern engine and sets up its HTTP proxy.
func (lc *LanternClient) runLantern(httpProxyAddr string, proxyAll bool) *flashlight.Flashlight {
	lc.mu.Lock()
	appName := lc.appName
	configDir := lc.configDir
	lc.mu.Unlock()

	log.Debugf("Starting Lantern with configDir=%s, proxyAll=%v", configDir, proxyAll)

	userConfig := common.NewUserConfig("", "a34113", 381696446, "K1qttSsZruN", map[string]string{}, "")

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
	// Start Lantern in a separate goroutine
	go func() {
		runner.Run(
			httpProxyAddr, // listen for HTTP on provided address
			"127.0.0.1:0", // listen for SOCKS on random address
			func(c *client.Client) {
				lc.mu.Lock()
				defer lc.mu.Unlock()
				lc.client = c
			},
			nil, // onError
		)
	}()
	return runner
}
