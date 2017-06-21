/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package android.irail.be.hyperrail.util;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.widget.DatePicker;
import android.widget.TimePicker;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A datetimepicker will show a datepicker, followed by a timepicker, and make a callback with the selected datetime.
 */
public class DateTimePicker implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    private final Context context;
    private OnDateTimeSetListener listener;
    private int year, month, day;

    public DateTimePicker(Context context) {
        this.context = context;
    }

    /**
     * Set the callback listener. Replaces a previous listener, if any.
     *
     * @param listener The callback listener to register.
     */
    public void setListener(OnDateTimeSetListener listener) {
        this.listener = listener;
    }

    /**
     * Pick a date & time
     */
    public void pick() {
        pickDate();
    }

    /**
     * Show the time picker dialog
     */
    private void pickTime() {
        final Calendar c = Calendar.getInstance();
        TimePickerDialog dialog = new TimePickerDialog(context, this, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true);
        dialog.show();
    }

    /**
     * Show the date picker dialog
     */
    private void pickDate() {
        final Calendar c = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(context, this, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    /**
     * Callback from time dialog
     *
     * @inheritDoc
     */
    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        Date date = new GregorianCalendar(this.year, this.month, this.day, hourOfDay, minute, 0).getTime();
        listener.onDateTimePicked(date);
    }

    /**
     * Callback from date dialog
     *
     * @inheritDoc
     */
    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
        pickTime();
    }
}
