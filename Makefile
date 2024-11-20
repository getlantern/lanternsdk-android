SDK_NAME := liblantern
SDK_DIR := sdk
GO_LIB_PATH := lantern
AAR_OUTPUT := lanternsdk-android.aar
LIBS_DIR := $(SDK_DIR)/libs
BUILD_DIR := build
PROD_FLAG := -ldflags "-s -w"
GRADLEW := ./gradlew
EXAMPLE_PROJECT := :example

.PHONY: build release clean publish example-debug

update-lantern-lib:
	git submodule update --init --recursive --remote

build:
	@echo "Building Lantern library..."
	cd $(GO_LIB_PATH) && \
	make android-sdk && \
	echo "Copying AAR to $(LIBS_DIR)..."
	mkdir -p $(LIBS_DIR)
	cp $(GO_LIB_PATH)/$(SDK_NAME)-all.aar $(LIBS_DIR)/$(AAR_OUTPUT)

# Build SDK release
release:
	@echo "Building production SDK.."
	$(GRADLEW) clean assembleRelease

sdk-debug:
	$(GRADLEW) clean :$(SDK_DIR):assembleDebug

example-debug:
	$(GRADLEW) clean $(EXAMPLE_PROJECT):assembleDebug

clean:
	rm -rf $(LIBS_DIR)/$(AAR_OUTPUT)
	rm -rf $(BUILD_DIR)
