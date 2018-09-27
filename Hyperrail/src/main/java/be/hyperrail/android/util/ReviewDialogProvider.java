/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.perf.metrics.AddTrace;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import be.hyperrail.android.R;

public class ReviewDialogProvider {

    private final static String KEY_DONT_SHOW_AGAIN = "rvd_dont_show_again";

    private static SharedPreferences sharedPreferences;
    private static int launches;
    private static long firstLaunch;

    private static FirebaseAnalytics mFirebaseAnalytics;

    @AddTrace(name = "reviewDialogProvider.init")
    public static void init(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        launches = sharedPreferences.getInt("rvd_app_launches", 0);
        firstLaunch = sharedPreferences.getLong("rvd_app_firstlaunch", 0);

        if (firstLaunch == 0) {
            firstLaunch = DateTime.now().getMillis();
        }

        sharedPreferences.edit().putInt("rvd_app_launches", launches + 1).putLong("rvd_app_firstlaunch", firstLaunch).apply();
    }


    public static void showDialogIf(Context context, int daysSinceInstall, int minLaunches) {

        if (sharedPreferences == null || sharedPreferences.getBoolean(KEY_DONT_SHOW_AGAIN, false)) {
            return;
        }
        DateTime installDate = new DateTime(firstLaunch);
        Log.i("RateDialog", (new Duration(installDate, DateTime.now())).getStandardHours() + "h since install. Rate dialog will show starting day " + daysSinceInstall);
        if ((new Duration(installDate, DateTime.now())).getStandardDays() >= daysSinceInstall && launches >= minLaunches) {
            // Show dialog
            showRateDialog(context);
        }
    }

    private static void showRateDialog(final Context context) {

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);


        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        int titleId = R.string.rate_dialog_title;
        int messageId = R.string.rate_dialog_message;
        int cancelButtonID = R.string.rate_dialog_button_later;
        int thanksButtonID = R.string.rate_dialog_button_no_thanks;
        int rateButtonID = R.string.rate_dialog_button_rate;
        builder.setTitle(titleId);
        builder.setMessage(messageId);

        builder.setPositiveButton(rateButtonID, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                String appPackage = context.getPackageName();
                String url = "market://details?id=" + appPackage;

                try {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + context.getPackageName())));
                }

                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.CAMPAIGN, "rate_dialog");
                bundle.putString(FirebaseAnalytics.Param.SUCCESS, "yes");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.CAMPAIGN_DETAILS, bundle);

                setOptOut(true);
            }
        });
        builder.setNeutralButton(cancelButtonID, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // reset the first launch timer, so it won't show again the next day(s)
                firstLaunch = DateTime.now().getMillis();
                sharedPreferences.edit().putLong("rvd_app_firstlaunch", firstLaunch).putInt("rvd_app_launches", 0).apply();

                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.CAMPAIGN, "rate_dialog");
                bundle.putString(FirebaseAnalytics.Param.SUCCESS, "later");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.CAMPAIGN_DETAILS, bundle);
            }
        });
        builder.setNegativeButton(thanksButtonID, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.CAMPAIGN, "rate_dialog");
                bundle.putString(FirebaseAnalytics.Param.SUCCESS, "no");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.CAMPAIGN_DETAILS, bundle);

                setOptOut(true);
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.CAMPAIGN, "rate_dialog");
                bundle.putString(FirebaseAnalytics.Param.SUCCESS, "dismiss");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.CAMPAIGN_DETAILS, bundle);
                // reset the first launch timer, so it won't show again the next day(s)
                firstLaunch = DateTime.now().getMillis();
                sharedPreferences.edit().putLong("rvd_app_firstlaunch", firstLaunch).putInt("rvd_app_launches", 0).apply();
            }
        });

        builder.show();
    }

    private static void setOptOut(boolean optOut) {
        if (sharedPreferences != null) {
            sharedPreferences.edit().putBoolean(KEY_DONT_SHOW_AGAIN, optOut).apply();
        }
    }

}
