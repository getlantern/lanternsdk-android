module github.com/getlantern/lanternsdk-android

go 1.22.4

replace github.com/tetratelabs/wazero => github.com/refraction-networking/wazero v1.7.1-w

replace github.com/getlantern/lantern-client => github.com/getlantern/lantern-client v0.0.0-20250122020443-3eec489d072c

require golang.org/x/mobile v0.0.0-20250106192035-c31d5b91ecc3

require (
	github.com/getlantern/lantern-client v0.0.0-20250122020443-3eec489d072c // indirect
	golang.org/x/mod v0.22.0 // indirect
	golang.org/x/sync v0.10.0 // indirect
	golang.org/x/tools v0.29.0 // indirect
)
