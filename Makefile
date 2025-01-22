SDK_NAME := liblantern
SDK_DIR := sdk
LIBLANTERN := liblantern.aar
SDK_OUTPUT := lanternsdk-android.aar
LIBS_DIR := $(SDK_DIR)/libs
BUILD_DIR := build
PROD_FLAG := -ldflags "-s -w"
GRADLEW := ./gradlew
EXAMPLE_PROJECT := :example

.PHONY: build release clean publish example-debug clone-go-client build build-aar copy-aar

build-aar: export EXTRA_LDFLAGS += -checklinkname=0
build-aar:
	@echo "Building Lantern library..."
	go env -w 'GOPRIVATE=github.com/getlantern/*' && \
	go install golang.org/x/mobile/cmd/gomobile@latest && \
	go get golang.org/x/mobile/bind && \
	gomobile init && \
	gomobile bind -target=android -tags='headless lantern' -o=$(BUILD_DIR)/$(LIBLANTERN) \
		-androidapi=23 \
		-ldflags="-s -w $(EXTRA_LDFLAGS)" \
		github.com/getlantern/lantern-client/sdk

copy-aar:
	echo "Copying AAR to $(LIBS_DIR)..."
	mkdir -p $(LIBS_DIR)
	cp $(BUILD_DIR)/$(LIBLANTERN) $(LIBS_DIR)/$(LIBLANTERN)

build: build-aar copy-aar
	echo "Lantern SDK: build/$(SDK_OUTPUT)"

# Build SDK
release: build sdk-release copy-sdk example-release

copy-sdk:
	cp build/$(SDK_OUTPUT) example/libs

sdk-debug:
	$(GRADLEW) clean :$(SDK_DIR):assembleDebug

sdk-release:
	$(GRADLEW) clean :$(SDK_DIR):assembleRelease
	cp build/sdk/outputs/aar/sdk-release.aar build/$(SDK_OUTPUT)

example-debug:
	$(GRADLEW) $(EXAMPLE_PROJECT):assembleDebug

example-release:
	$(GRADLEW) $(EXAMPLE_PROJECT):assembleRelease

clean:
	rm -rf $(LIBS_DIR)/$(LIBLANTERN)
	rm -rf $(BUILD_DIR)
