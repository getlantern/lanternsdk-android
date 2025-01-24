
LIBLANTERN := liblantern
SDK_NAME := lanternsdk-android
SDK_DIR := sdk
SDK_OUTPUT := $(SDK_NAME).aar
BUILD_DIR := build
PROD_FLAG := -ldflags "-s -w"
GRADLEW := ./gradlew
AAR_EXPLODED := $(BUILD_DIR)/aar-exploded
SDK_JNI_LIBS := $(SDK_DIR)/src/main/jniLibs
SDK_LIBS := $(SDK_DIR)/libs
SDK_OUTPUT_AAR := build/$(SDK_DIR)/outputs/aar/sdk-release.aar
EXAMPLE_PROJECT := :example
VERSION ?= dev

.PHONY: build-aar install-aar sdk-release build-sdk example-debug example-release release clean

build-aar: export EXTRA_LDFLAGS += -checklinkname=0
build-aar:
	@echo "Building Lantern library..."
	mkdir -p $(BUILD_DIR)
	go env -w 'GOPRIVATE=github.com/getlantern/*'
	go install golang.org/x/mobile/cmd/gomobile@latest
	go get golang.org/x/mobile/bind
	gomobile init
	gomobile bind -target=android -tags='headless lantern' \
		-o=$(BUILD_DIR)/$(LIBLANTERN).aar \
		-androidapi=23 \
		-ldflags="-s -w -checklinkname=0" \
		./lantern

## Copy the generated .aar into the SDK module's libs folder
install-aar:
	echo "Copying AAR to $(SDK_LIBS)..."
	mkdir -p $(SDK_LIBS)
	cp $(BUILD_DIR)/$(LIBLANTERN) $(SDK_LIBS)/$(LIBLANTERN)

sdk-release:
	@echo "Building SDK module..."
	@$(GRADLEW) :$(SDK_DIR):assembleRelease
	@echo "SDK release built."

extract: build-aar
	rm -rf $(AAR_EXPLODED)
	mkdir -p $(AAR_EXPLODED)
	unzip -o $(BUILD_DIR)/$(LIBLANTERN).aar -d $(AAR_EXPLODED)

merge-libs: extract
	mkdir -p $(SDK_JNI_LIBS)
	cp -R $(AAR_EXPLODED)/jni/* $(SDK_JNI_LIBS) || true

	mkdir -p $(SDK_LIBS)
	cp $(AAR_EXPLODED)/classes.jar $(SDK_LIBS)/$(LIBLANTERN).jar

# Build SDK
build-sdk: merge-libs sdk-release
	@echo "Copying final SDK AAR..."
	cp build/$(SDK_DIR)/outputs/aar/sdk-release.aar \
	   $(BUILD_DIR)/lanternsdk-android-$(VERSION).aar
	@echo "SDK built -> $(BUILD_DIR)/lanternsdk-android-$(VERSION).aar"

example-debug:
	mkdir -p example/libs
	cp $(BUILD_DIR)/lanternsdk-android-$(VERSION).aar example/libs/lanternsdk-android.aar
	$(GRADLEW) $(EXAMPLE_PROJECT):assembleDebug

example-release:
	$(GRADLEW) $(EXAMPLE_PROJECT):assembleRelease

release: build-sdk example-release
	@echo "Release complete. AAR is at $(BUILD_DIR)/lanternsdk-android-$(VERSION).aar"

clean:
	rm -rf $(BUILD_DIR)
	rm -rf $(LIBS_DIR)
	@$(GRADLEW) clean
