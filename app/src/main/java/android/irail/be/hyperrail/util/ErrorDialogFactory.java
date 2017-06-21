/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package android.irail.be.hyperrail.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.irail.be.hyperrail.R;
import android.irail.be.hyperrail.irail.exception.InvalidResponseException;
import android.irail.be.hyperrail.irail.exception.NetworkDisconnectedException;
import android.irail.be.hyperrail.irail.exception.NotFoundException;
import android.support.annotation.StringRes;

/**
 * This class groups error dialogs for API results, to ensure consistent errors
 */
public class ErrorDialogFactory {

    /**
     * Show an error dialog based on the exception
     * @param exception The exception which occured
     * @param context The current context
     * @param finish Whether or not to finish this activity
     * @return The dialog which is shown
     */
    public static AlertDialog showErrorDialog(final Exception exception, final Activity context, final boolean finish) {
        if (exception instanceof NetworkDisconnectedException) {
            return showNetworkErrorDialog(context, finish);
        } else if (exception instanceof InvalidResponseException) {
            return showServerErrorDialog(context, finish);
        } else if (exception instanceof NotFoundException || exception instanceof java.io.FileNotFoundException) {
            return showNotFoundErrorDialog(context, finish);
        } else {
            return showGeneralErrorDialog(context, finish);
        }
    }
    /**
     * Show a generic error dialog
     * @param context The current context
     * @param finish Whether or not to finish this activity
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
     * @param context The current context
     * @param finish Whether or not to finish this activity
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
     * @param context The current context
     * @param finish Whether or not to finish this activity
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
     * @param context The current context
     * @param finish Whether or not to finish this activity
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
     * @param context The current context
     * @param finish Whether or not to finish this activity
     * @return The dialog which is shown
     */
    public static AlertDialog showInvalidDepartureStationError(final Activity context, final boolean finish) {
        return ErrorDialogFactory.showCustomDialog(context, R.string.error_departure_not_found_title, R.string.error_departure_not_found_message, finish);
    }
    /**
     * Show an error in case of an invalid arrival station
     * @param context The current context
     * @param finish Whether or not to finish this activity
     * @return The dialog which is shown
     */
    public static AlertDialog showInvalidDestinationStationError(final Activity context, final boolean finish) {
        return ErrorDialogFactory.showCustomDialog(context, R.string.error_destination_not_found_title, R.string.error_destination_not_found_message, finish);
    }
    /**
     * Show a custom error dialog
     * @param context The current context
     * @param finish Whether or not to finish this activity
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
