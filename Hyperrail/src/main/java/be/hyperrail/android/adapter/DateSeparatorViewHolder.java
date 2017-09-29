/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import be.hyperrail.android.R;

public class DateSeparatorViewHolder extends RecyclerView.ViewHolder {

    protected final TextView vDateText;

    DateSeparatorViewHolder(View view) {
        super(view);
        vDateText = view.findViewById(R.id.text_date);
    }
}