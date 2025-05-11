/*
 * Copyright (C) 2023-2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.xiaomi.settings;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.Display.HdrCapabilities;
import vendor.xiaomi.hw.touchfeature.ITouchFeature;

import com.xiaomi.settings.display.ColorModeService;
import com.xiaomi.settings.refreshrate.RefreshUtils;
import com.xiaomi.settings.touchsampling.TouchSamplingUtils;
import com.xiaomi.settings.touchsampling.TouchSamplingService;
import com.xiaomi.settings.touchsampling.TouchSamplingTileService;
import com.xiaomi.settings.turbocharging.TurboChargingService;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "XiaomiParts";
    private static final boolean DEBUG = true;
    private static final int DOUBLE_TAP_TO_WAKE_MODE = 14;
    private static final int Touch_Fod_Enable     = 10;
    private static final int Touch_Aod_Enable     = 11;
    private static final int Touch_FodIcon_Enable = 16;

    private ITouchFeature xiaomiTouchFeatureAidl;

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (DEBUG) Log.i(TAG, "Received intent: " + intent.getAction());
        switch (intent.getAction()) {
            case Intent.ACTION_LOCKED_BOOT_COMPLETED:
                onLockedBootCompleted(context);
                break;
            case Intent.ACTION_BOOT_COMPLETED:
                onBootCompleted(context);
                break;
        }
    }

    /**
     * Handles actions to perform after a locked boot is completed.
     * This method is now non-static, allowing access to instance methods.
     */
    private void onLockedBootCompleted(Context context) {
        // Start ColorModeService
        context.startServiceAsUser(new Intent(context, ColorModeService.class),
                UserHandle.CURRENT);

        // High Touch polling rate
        TouchSamplingUtils.restoreSamplingValue(context);
        context.startServiceAsUser(new Intent(context, TouchSamplingService.class),
                UserHandle.CURRENT);

        // Refreshrate
        RefreshUtils.startService(context);

        // Touch Sampling Tile Service
        context.startServiceAsUser(new Intent(context, TouchSamplingTileService.class),
                UserHandle.CURRENT);

        // TurboChargingService
        context.startServiceAsUser(new Intent(context, TurboChargingService.class),
                UserHandle.CURRENT);

        // Override HDR types to enable Dolby Vision
        final DisplayManager displayManager = context.getSystemService(DisplayManager.class);
        if (displayManager != null) {
            displayManager.overrideHdrTypes(Display.DEFAULT_DISPLAY,
                    new int[] {
                        HdrCapabilities.HDR_TYPE_DOLBY_VISION,
                        HdrCapabilities.HDR_TYPE_HDR10,
                        HdrCapabilities.HDR_TYPE_HLG,
                        HdrCapabilities.HDR_TYPE_HDR10_PLUS
                    });
        } else {
            Log.e(TAG, "DisplayManager service not available");
        }

        // force-enable SoFOD on lock screen
        initTouchFeatureService();
        try {
            if (xiaomiTouchFeatureAidl != null) {
            xiaomiTouchFeatureAidl.setTouchMode(0, Touch_Fod_Enable, 1);
            xiaomiTouchFeatureAidl.setTouchMode(0, Touch_Aod_Enable, 1);
            xiaomiTouchFeatureAidl.setTouchMode(0, Touch_FodIcon_Enable, 1);
            if (DEBUG) Log.i(TAG, "SoFOD features enabled on lock screen");
            }
        } catch (Exception e) {
          Log.e(TAG, "Error setting SoFOD touch mode", e);
        }

        // Create and register ContentObserver for DOUBLE_TAP_TO_WAKE setting
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                updateTapToWakeStatus(context);
            }
        };

        ContentResolver resolver = context.getContentResolver();
        resolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.DOUBLE_TAP_TO_WAKE), true, observer);

        // Update Tap to Wake status initially
        updateTapToWakeStatus(context);
    }

    /**
     * Updates the Tap to Wake status based on the system settings.
     * This is an instance method and can access instance variables.
     */
    private void updateTapToWakeStatus(Context context) {
        try {
            if (xiaomiTouchFeatureAidl == null) initTouchFeatureService();
            boolean enabled = Settings.Secure.getInt(
                                      context.getContentResolver(),
                                      Settings.Secure.DOUBLE_TAP_TO_WAKE, 0) == 1;

            if (xiaomiTouchFeatureAidl != null) {
                xiaomiTouchFeatureAidl.setTouchMode(0, DOUBLE_TAP_TO_WAKE_MODE, enabled ? 1 : 0);
                if (DEBUG) {
                    Log.i(TAG, "Tap to Wake set to " + (enabled ? "enabled" : "disabled"));
                }
            } else {
                Log.e(TAG, "Touch Feature AIDL interface is not available");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to update Tap to Wake status", e);
        }
    }

    private void initTouchFeatureService() {
        if (xiaomiTouchFeatureAidl != null) return;
        try {
            String name = "default";
            String fqName = ITouchFeature.DESCRIPTOR + "/" + name;
            IBinder binder = Binder.allowBlocking(
                    ServiceManager.waitForDeclaredService(fqName)
            );
            xiaomiTouchFeatureAidl = ITouchFeature.Stub.asInterface(binder);
            if (DEBUG) Log.i(TAG, "TouchFeature service connected");
        } catch (Exception e) {
            Log.e(TAG, "Failed to connect to TouchFeature service", e);
        }
    }

    /**
     * Handles actions to perform after a standard boot is completed.
     * This method is now non-static.
     */
    private void onBootCompleted(Context context) {
    }
}
