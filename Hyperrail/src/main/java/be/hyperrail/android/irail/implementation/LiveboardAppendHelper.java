/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.irail.implementation;

import org.joda.time.DateTime;

import be.hyperrail.android.irail.contracts.IRailErrorResponseListener;
import be.hyperrail.android.irail.contracts.IRailSuccessResponseListener;
import be.hyperrail.android.irail.contracts.IrailDataProvider;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.util.ArrayUtils;

public class LiveboardAppendHelper implements IRailSuccessResponseListener<LiveBoard>, IRailErrorResponseListener<LiveBoard> {

    private int attempt = 0;
    private DateTime lastSearchTime;
    private LiveBoard originalLiveboard;

    private IRailSuccessResponseListener<LiveBoard> successResponseListener;
    private IRailErrorResponseListener<LiveBoard> errorResponseListener;
    private Object tag;

    IrailDataProvider api = IrailFactory.getDataProviderInstance();

    public void appendLiveboard(final LiveBoard liveBoard, final IRailSuccessResponseListener<LiveBoard> successResponseListener,
                                final IRailErrorResponseListener<LiveBoard> errorResponseListener, final Object tag) {

        this.successResponseListener = successResponseListener;
        this.errorResponseListener = errorResponseListener;
        this.tag = tag;

        this.originalLiveboard = liveBoard;

        if (liveBoard.getStops().length > 0) {
            lastSearchTime = liveBoard.getStops()[liveBoard.getStops().length - 1].getDepartureTime().plusMinutes(1);
        } else {
            lastSearchTime = liveBoard.getSearchTime().plusHours(1);
        }

        api.getLiveboard(liveBoard, lastSearchTime, RouteTimeDefinition.DEPART, this, this, null);
    }

    @Override
    public void onSuccessResponse(LiveBoard data, Object tag) {
        if (data.getStops().length > 0) {
            TrainStop[] mergedStops = ArrayUtils.concatenate(originalLiveboard.getStops(), data.getStops());
            LiveBoard merged = new LiveBoard(originalLiveboard, mergedStops, originalLiveboard.getSearchTime());
            this.successResponseListener.onSuccessResponse(merged, tag);
        } else {
            attempt++;
            lastSearchTime = lastSearchTime.plusHours(1);

            if (attempt < 12) {
                api.getLiveboard(originalLiveboard, lastSearchTime, RouteTimeDefinition.DEPART, this, this, null);
            } else {
                if (this.successResponseListener != null) {
                    this.successResponseListener.onSuccessResponse(originalLiveboard, this.tag);
                }
            }
        }
    }

    @Override
    public void onErrorResponse(Exception e, Object tag) {
        if (this.errorResponseListener != null) {
            this.errorResponseListener.onErrorResponse(e, this.tag);
        }

    }
}
