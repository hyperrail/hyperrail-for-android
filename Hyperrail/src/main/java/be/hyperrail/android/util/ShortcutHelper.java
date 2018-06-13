/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.design.widget.Snackbar;
import android.view.View;

import be.hyperrail.android.R;

/**
 * Helper class to create shortcuts
 */
public class ShortcutHelper {

    public static void createShortcut(Context context, View layoutRoot, Intent intent, String defaultTitle, String longLabel, @DrawableRes int icon){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutInfo.Builder mShortcutInfoBuilder = new ShortcutInfo.Builder(context, defaultTitle);
            mShortcutInfoBuilder.setShortLabel(defaultTitle);
            mShortcutInfoBuilder.setLongLabel(longLabel);
            mShortcutInfoBuilder.setIcon(Icon.createWithResource(context, icon));
            intent.setAction(Intent.ACTION_VIEW);
            mShortcutInfoBuilder.setIntent(intent);
            ShortcutInfo mShortcutInfo = mShortcutInfoBuilder.build();
            ShortcutManager mShortcutManager = context.getSystemService(ShortcutManager.class);
            if (mShortcutManager != null) {
                mShortcutManager.requestPinShortcut(mShortcutInfo, null);
            }
        } else {
            Intent addIntent = new Intent();
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, defaultTitle);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context, icon));
            addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            context.sendBroadcast(addIntent);
        }
        Snackbar.make(layoutRoot, R.string.shortcut_created, Snackbar.LENGTH_LONG).show();

    }
}
