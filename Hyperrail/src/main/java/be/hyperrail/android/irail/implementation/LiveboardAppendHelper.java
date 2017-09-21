/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.irail.implementation;

import org.joda.time.DateTime;

import be.hyperrail.android.irail.contracts.IrailDataProvider;
import be.hyperrail.android.irail.contracts.IrailResponseListener;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.util.ArrayUtils;

public class LiveboardAppendHelper implements IrailResponseListener<LiveBoard> {

    private final static int APPEND_LIVEBOARD = 0;

    private int attempt = 0;
    private DateTime lastSearchTime;
    private LiveBoard originalLiveboard;
    private IrailResponseListener<LiveBoard> callback;
    private int tag;
    IrailDataProvider api = IrailFactory.getDataProviderInstance();

    public void appendLiveboard(final IrailResponseListener<LiveBoard> callback, final int tag, final LiveBoard liveBoard) {
        this.callback = callback;
        this.tag = tag;
        this.originalLiveboard = liveBoard;

        if (liveBoard.getStops().length > 0) {
            lastSearchTime = liveBoard.getStops()[liveBoard.getStops().length - 1].getDepartureTime().plusMinutes(1);
        } else {
            lastSearchTime = liveBoard.getSearchTime().plusHours(1);
        }
        api.getLiveboard(this, APPEND_LIVEBOARD, liveBoard, lastSearchTime);
    }

    @Override
    public void onIrailSuccessResponse(LiveBoard data, int tag) {
        switch (tag) {
            case APPEND_LIVEBOARD:
                if (data.getStops().length > 0) {
                    TrainStop[] mergedStops = ArrayUtils.concatenate(originalLiveboard.getStops(), data.getStops());
                    LiveBoard merged = new LiveBoard(originalLiveboard, mergedStops, originalLiveboard.getSearchTime());
                    callback.onIrailSuccessResponse(merged, tag);
                } else {
                    attempt++;
                    lastSearchTime = lastSearchTime.plusHours(1);
                    if (attempt < 12) {
                        api.getLiveboard(this, APPEND_LIVEBOARD, originalLiveboard, lastSearchTime);
                    } else {
                        this.callback.onIrailSuccessResponse(originalLiveboard, this.tag);
                    }
                }
        }
    }

    @Override
    public void onIrailErrorResponse(Exception e, int tag) {
        switch (tag) {
            case APPEND_LIVEBOARD:
                callback.onIrailErrorResponse(e, this.tag);
                break;
        }
    }
}
