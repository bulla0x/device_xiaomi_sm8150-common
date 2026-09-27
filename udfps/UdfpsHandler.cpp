/*
 * Copyright (C) 2022 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#define LOG_TAG "UdfpsHandler.xiaomi_msmnile"

#include "UdfpsHandler.h"

#include <android-base/logging.h>
#include <fcntl.h>
#include <poll.h>
#include <thread>
#include <unistd.h>

#define COMMAND_NIT 10
#define PARAM_NIT_FOD 1
#define PARAM_NIT_NONE 0

/*
 * Illumination is the kernel's job. When the UDFPS overlay is pressed,
 * SurfaceFlinger tags its layer with FOD_PRESSED_LAYER_ZORDER, the display HAL
 * carries the bit onto the DRM plane (FOD_ZPOS), and sde_crtc builds the FOD
 * dim layer from PLANE_PROP_FOD: the panel goes to HBM with a compensating dim
 * layer under the circle, frame-synchronised under panel_lock, and
 * sde_connector_update_fod_hbm() raises fod_ui. All this handler has to do is
 * tell the sensor when the light is actually there.
 *
 * Driving fod_hbm and the backlight from here was needed while the sm8350
 * display HAL dropped the FOD bit (changes.md sections 22, 31, 34). With the
 * sm8150 display HAL live (section 38) the in-kernel path works again; see
 * section 51 for the measurements.
 */
static const char* kFodUiPaths[] = {
        "/sys/devices/platform/soc/soc:qcom,dsi-display-primary/fod_ui",
        "/sys/devices/platform/soc/soc:qcom,dsi-display/fod_ui",
};

/*
 * Arms the goodix touch IC to report FOD-area presses (BTN_INFO), which is
 * what makes the HAL's finger-down detection fire at all. init.xiaomi.rc arms
 * it at boot; it is re-asserted here and never written back to 0.
 */
static const char* kFodStatusPaths[] = {
        "/sys/touchpanel/fod_status",
        "/sys/devices/virtual/touch/tp_dev/fod_status",
};

static bool readBool(int fd) {
    char c;
    int rc;

    rc = lseek(fd, 0, SEEK_SET);
    if (rc) {
        LOG(ERROR) << "failed to seek fd, err: " << rc;
        return false;
    }

    rc = read(fd, &c, sizeof(char));
    if (rc != 1) {
        LOG(ERROR) << "failed to read bool from fd, err: " << rc;
        return false;
    }

    return c != '0';
}

static void writeBool(int fd, bool value) {
    if (fd < 0) {
        return;
    }

    lseek(fd, 0, SEEK_SET);
    if (write(fd, value ? "1" : "0", 1) < 0) {
        LOG(ERROR) << "failed to write " << value << " to fd " << fd;
    }
}

class XiaomiMsmnileUdfpsHandler : public UdfpsHandler {
  public:
    void init(fingerprint_device_t *device) {
        mDevice = device;

        for (auto& path : kFodStatusPaths) {
            mFodStatusFd = open(path, O_WRONLY);
            if (mFodStatusFd >= 0) {
                break;
            }
        }
        writeBool(mFodStatusFd, true);

        std::thread([this]() {
            int fodUiFd = -1;
            for (auto& path : kFodUiPaths) {
                fodUiFd = open(path, O_RDONLY);
                if (fodUiFd >= 0) {
                    break;
                }
            }

            if (fodUiFd < 0) {
                LOG(ERROR) << "failed to open fd, err: " << fodUiFd;
                return;
            }

            struct pollfd fodUiPoll = {
                    .fd = fodUiFd,
                    .events = POLLERR | POLLPRI,
                    .revents = 0,
            };

            while (true) {
                int rc = poll(&fodUiPoll, 1, -1);
                if (rc < 0) {
                    LOG(ERROR) << "failed to poll fd, err: " << rc;
                    continue;
                }

                bool fodUi = readBool(fodUiFd);
                LOG(INFO) << "fod_ui changed to " << fodUi;

                // The panel is lit (or not); let the sensor know.
                if (mDevice && mDevice->extCmd) {
                    mDevice->extCmd(mDevice, COMMAND_NIT,
                                    fodUi ? PARAM_NIT_FOD : PARAM_NIT_NONE);
                }
                // Keep the touch IC armed across sessions.
                if (fodUi) {
                    writeBool(mFodStatusFd, true);
                }
            }
        }).detach();
    }

    void onFingerDown(uint32_t /*x*/, uint32_t /*y*/, float /*minor*/, float /*major*/) {
        LOG(INFO) << "onFingerDown";
    }

    void onFingerUp() {
        LOG(INFO) << "onFingerUp";
    }

    void onAcquired(int32_t /*result*/, int32_t /*vendorCode*/) {
        // nothing
    }

    void cancel() {
        LOG(INFO) << "cancel";
        if (mDevice && mDevice->extCmd) {
            mDevice->extCmd(mDevice, COMMAND_NIT, PARAM_NIT_NONE);
        }
    }

  private:
    fingerprint_device_t *mDevice = nullptr;
    int mFodStatusFd = -1;
};

static UdfpsHandler* create() {
    return new XiaomiMsmnileUdfpsHandler();
}

static void destroy(UdfpsHandler* handler) {
    delete handler;
}

extern "C" UdfpsHandlerFactory UDFPS_HANDLER_FACTORY = {
        .create = create,
        .destroy = destroy,
};
