/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.annotation.StringRes;
import android.util.Log;

import com.android.volley.NoConnectionError;
import com.android.volley.ServerError;

import be.hyperrail.android.R;

/**
 * This class groups error dialogs for API results, to ensure consistent errors
 */
public class ErrorDialogFactory {

    /**
     * Show an error dialog based on the exception
     *
     * @param exception The exception which occurred
     * @param context   The current context
     * @param finish    Whether or not to finish this activity
     */
    public static AlertDialog showErrorDialog(Exception exception, Activity context, boolean finish) {
        if (context == null || context.isFinishing()) {
            // No valid context/activity to show this dialog
            Log.w("ErrorDialogFactory", "Failed to show error dialog: Activity is already finishing or finished");
            return null;
        }

        if (exception instanceof ServerError) {
            if (((ServerError) exception).networkResponse != null) {
                if (((ServerError) exception).networkResponse.statusCode == 404) {
                    return showNotFoundErrorDialog(context, finish);
                } else if (((ServerError) exception).networkResponse.statusCode == 500) {
                    return showServerErrorDialog(context, finish);
                } else {
                    return showServerErrorDialog(context, finish);
                }
            } else {
                return showGeneralErrorDialog(context, finish);
            }
        } else if (exception instanceof NoConnectionError) {
            return showNetworkErrorDialog(context, finish);
        } else {
            return showGeneralErrorDialog(context, finish);
        }
    }

    /**
     * Show a generic error dialog
     *
     * @param context The current context
     * @param finish  Whether or not to finish this activity
     * @return The dialog which is shown
     */

    private static AlertDialog showGeneralErrorDialog(final Activity context, final boolean finish) {
        return new AlertDialog.Builder(context)
                .setTitle(R.string.error_general_title)
                .setMessage(R.string.error_general_message)
                .setOnDismissListener(
                        new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                if (finish && context != null && !context.isFinishing()) {
                                    context.finish();
                                }
                            }
                        }
                ).show();
    }

    /**
     * Show a network error dialog
     *
     * @param context The current context
     * @param finish  Whether or not to finish this activity
     * @return The dialog which is shown
     */
    private static AlertDialog showNetworkErrorDialog(final Activity context, final boolean finish) {
        return new AlertDialog.Builder(context)
                .setTitle(R.string.error_network_title)
                .setMessage(R.string.error_network_message)
                .setOnDismissListener(
                        new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                if (finish && context != null && !context.isFinishing()) {
                                    context.finish();
                                }
                            }
                        }
                ).show();
    }

    /**
     * Show a server error dialog
     *
     * @param context The current context
     * @param finish  Whether or not to finish this activity
     * @return The dialog which is shown
     */
    private static AlertDialog showServerErrorDialog(final Activity context, final boolean finish) {
        return new AlertDialog.Builder(context)
                .setTitle(R.string.error_servererror_title)
                .setMessage(R.string.error_servererror_message)
                .setOnDismissListener(
                        new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                if (finish && context != null && !context.isFinishing()) {
                                    context.finish();
                                }
                            }
                        }
                ).show();
    }

    /**
     * Show a not found error dialog
     *
     * @param context The current context
     * @param finish  Whether or not to finish this activity
     * @return The dialog which is shown
     */
    private static AlertDialog showNotFoundErrorDialog(final Activity context, final boolean finish) {
        return new AlertDialog.Builder(context)
                .setTitle(R.string.error_notfound_title)
                .setMessage(R.string.error_notfound_message)
                .setOnDismissListener(
                        new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                if (finish && context != null && !context.isFinishing()) {
                                    context.finish();
                                }
                            }
                        }
                ).show();
    }

    /**
     * Show an error in case of an invalid departure station
     *
     * @param context The current context
     * @param finish  Whether or not to finish this activity
     */
    public static void showInvalidDepartureStationError(Activity context, boolean finish) {
        showCustomDialog(context, R.string.error_departure_not_found_title, R.string.error_departure_not_found_message, finish);
    }

    /**
     * Show an error in case of an invalid arrival station
     *
     * @param context The current context
     * @param finish  Whether or not to finish this activity
     */
    public static void showInvalidDestinationStationError(Activity context, boolean finish) {
        showCustomDialog(context, R.string.error_destination_not_found_title, R.string.error_destination_not_found_message, finish);
    }

    /**
     * Show an error in case the departure station equals the arrival station
     *
     * @param context The current context
     * @param finish  Whether or not to finish this activity
     */
    public static void showDepartureEqualsArrivalStationError(Activity context, boolean finish) {
        showCustomDialog(context, R.string.error_departure_equals_destination, R.string.error_departure_equals_destination_message, finish);
    }

    /**
     * Show a custom error dialog
     *
     * @param context The current context
     * @param finish  Whether or not to finish this activity
     * @return The dialog which is shown
     */
    private static AlertDialog showCustomDialog(final Activity context, @StringRes int title, @StringRes int message, final boolean finish) {
        return new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setOnDismissListener(
                        new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                if (finish && context != null && !context.isFinishing()) {
                                    context.finish();
                                }
                            }
                        }
                ).show();
    }

}
