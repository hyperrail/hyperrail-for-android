/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.joda.time.DateTime;

import be.hyperrail.android.R;
import be.hyperrail.android.logging.HyperRailLog;

public class CrashlyticsOptInDialog {

    private static final String PREFERENCES_KEY_OPTIN_REPLIED = "crashreporting_optin_replied";
    private static final String PREFERENCES_CRASHLYTICS_ENABLED = "pref_crashlytics_enabled";

    private static final HyperRailLog log = HyperRailLog.getLogger(CrashlyticsOptInDialog.class);
    private static SharedPreferences sharedPreferences;
    private static boolean hasOptedInOrOut;

    private CrashlyticsOptInDialog() {
        // No public constructor
    }

    public static void init(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        hasOptedInOrOut = sharedPreferences.getBoolean(PREFERENCES_KEY_OPTIN_REPLIED, false);
    }


    public static void showDialogOnce(Context context) {
        if (!hasOptedInOrOut){
            showOptInDialog(context);
        }
    }

    private static void showOptInDialog(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        int titleId = R.string.crashlytics_dialog_title;
        int messageId = R.string.crashlytics_dialog_message;
        int disableButtonText = R.string.crashlytics_dialog_button_disable;
        int enableButtonText = R.string.crashlytics_dialog_button_enable;
        builder.setTitle(titleId);
        builder.setMessage(messageId);
        setDialogButtonActions(builder, enableButtonText, disableButtonText);
        builder.show();
    }

    private static void setDialogButtonActions(AlertDialog.Builder builder, int enableButtonText, int disableButtonText) {
        builder.setPositiveButton(enableButtonText, (dialog, which) -> {
            optIn();
            setOptedInOrOut();
        });
        builder.setNegativeButton(disableButtonText, (dialog, which) -> {
            optOut();
            setOptedInOrOut();
        });
    }

    private static void optIn() {
        log.info("Storing opt-in from crashlytics");
        setPreference(PREFERENCES_CRASHLYTICS_ENABLED, true);
    }
    private static void optOut() {
        log.info("Storing opt-out from crashlytics");
        setPreference(PREFERENCES_CRASHLYTICS_ENABLED, false);
    }

    private static void setOptedInOrOut() {
        setPreference(PREFERENCES_KEY_OPTIN_REPLIED, true);
    }

    private static void setPreference(String preference, boolean value) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().putBoolean(preference, value).apply();
        }
    }

}
