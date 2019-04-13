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
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import androidx.annotation.DrawableRes;
import com.google.android.material.snackbar.Snackbar;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import be.hyperrail.android.R;

/**
 * Helper class to create shortcuts
 */
public class ShortcutHelper {

    private ShortcutHelper(){
        // No public constructor
    }

    public static void createShortcut(final Context context, final View layoutRoot, final Intent intent, String defaultTitle, final String longLabel, @DrawableRes final int icon) {

        // Ask users if they want to set a custom title
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.dialog_title_shortcut_name);

        // Set up the input
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(defaultTitle);

        FrameLayout container = new FrameLayout(context);
        FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = context.getResources().getDimensionPixelSize(R.dimen.alertdialog_horizontal_margin);
        params.rightMargin = context.getResources().getDimensionPixelSize(R.dimen.alertdialog_horizontal_margin);
        input.setLayoutParams(params);
        container.addView(input);

        builder.setView(container);

        // Set up the buttons
        builder.setPositiveButton(R.string.button_add, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                addShortcut(context, layoutRoot, intent, input.getText().toString(), longLabel, icon);
            }
        });
        builder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();

    }

    private static void addShortcut(Context context, View layoutRoot, Intent intent, String shortLabel, String longLabel, @DrawableRes int icon) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ShortcutInfo.Builder mShortcutInfoBuilder = new ShortcutInfo.Builder(context, shortLabel);
            mShortcutInfoBuilder.setShortLabel(shortLabel);
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
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortLabel);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context, icon));
            addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            context.sendBroadcast(addIntent);
        }
        Snackbar.make(layoutRoot, R.string.shortcut_created, Snackbar.LENGTH_LONG).show();
    }
}
