/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import be.hyperrail.android.R;
import be.hyperrail.android.logging.HyperRailLog;

public class ReviewDialogProvider {

    private static final String PREFERENCES_KEY_DONT_SHOW_AGAIN = "rvd_dont_show_again";
    private static final String PREFERENCES_KEY_FIRST_LAUNCH = "rvd_app_firstlaunch";
    private static final String PREFERENCES_KEY_LAUNCH_COUNT = "rvd_app_launches";

    private static final HyperRailLog log = HyperRailLog.getLogger(ReviewDialogProvider.class);

    private static SharedPreferences sharedPreferences;
    private static int launches;
    private static long firstLaunch;

    private ReviewDialogProvider() {
        // No public constructor
    }

    public static void init(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        launches = sharedPreferences.getInt(PREFERENCES_KEY_LAUNCH_COUNT, 0);
        firstLaunch = sharedPreferences.getLong(PREFERENCES_KEY_FIRST_LAUNCH, 0);

        if (firstLaunch == 0) {
            firstLaunch = DateTime.now().getMillis();
        }

        sharedPreferences.edit().putInt(PREFERENCES_KEY_LAUNCH_COUNT, launches + 1).putLong(PREFERENCES_KEY_FIRST_LAUNCH, firstLaunch).apply();
    }


    public static void showDialogIf(Context context, int daysSinceInstall, int minLaunches) {

        if (sharedPreferences == null || sharedPreferences.getBoolean(PREFERENCES_KEY_DONT_SHOW_AGAIN, false)) {
            return;
        }
        DateTime installDate = new DateTime(firstLaunch);
        log.info((new Duration(installDate, DateTime.now())).getStandardHours() + "h since install. Rate dialog will show starting day " + daysSinceInstall);
        if ((new Duration(installDate, DateTime.now())).getStandardDays() >= daysSinceInstall && launches >= minLaunches) {
            // Show dialog
            showRateDialog(context);
        }
    }

    private static void showRateDialog(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        int titleId = R.string.rate_dialog_title;
        int messageId = R.string.rate_dialog_message;
        int cancelButtonText = R.string.rate_dialog_button_later;
        int thanksButtonText = R.string.rate_dialog_button_no_thanks;
        int rateButtonText = R.string.rate_dialog_button_rate;
        builder.setTitle(titleId);
        builder.setMessage(messageId);

        setDialogButtonActions(context, builder, cancelButtonText, thanksButtonText, rateButtonText);

        builder.show();
    }

    private static void setDialogButtonActions(Context context, AlertDialog.Builder builder, int cancelButtonText, int thanksButtonText, int rateButtonText) {
        builder.setPositiveButton(rateButtonText, (dialog, which) -> {
            String appPackage = context.getPackageName();
            String url = "market://details?id=" + appPackage;

            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (android.content.ActivityNotFoundException anfe) {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + context.getPackageName())));
            }
            setOptOut(true);
        });
        builder.setNeutralButton(cancelButtonText, (dialog, which) -> {
            // reset the first launch timer, so it won't show again the next day(s)
            firstLaunch = DateTime.now().getMillis();
            sharedPreferences.edit().putLong(PREFERENCES_KEY_FIRST_LAUNCH, firstLaunch).putInt(PREFERENCES_KEY_LAUNCH_COUNT, 0).apply();
        });
        builder.setNegativeButton(thanksButtonText, (dialog, which) -> {
            setOptOut(true);
        });
        builder.setOnCancelListener(dialog -> {
            // reset the first launch timer, so it won't show again the next day(s)
            firstLaunch = DateTime.now().getMillis();
            sharedPreferences.edit().putLong(PREFERENCES_KEY_FIRST_LAUNCH, firstLaunch).putInt(PREFERENCES_KEY_LAUNCH_COUNT, 0).apply();
        });
    }

    private static void setOptOut(boolean optOut) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().putBoolean(PREFERENCES_KEY_DONT_SHOW_AGAIN, optOut).apply();
        }
    }

}
