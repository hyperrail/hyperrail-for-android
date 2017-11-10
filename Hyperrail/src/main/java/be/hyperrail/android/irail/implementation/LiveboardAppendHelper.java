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

    private final int TAG_APPEND = 0;
    private final int TAG_PREPEND = 1;

    private int attempt = 0;
    private DateTime lastSearchTime;
    private LiveBoard originalLiveboard;

    private IRailSuccessResponseListener<LiveBoard> successResponseListener;
    private IRailErrorResponseListener<LiveBoard> errorResponseListener;

    IrailDataProvider api = IrailFactory.getDataProviderInstance();

    public void appendLiveboard(final LiveBoard liveBoard, final IRailSuccessResponseListener<LiveBoard> successResponseListener,
                                final IRailErrorResponseListener<LiveBoard> errorResponseListener) {

        this.successResponseListener = successResponseListener;
        this.errorResponseListener = errorResponseListener;

        this.originalLiveboard = liveBoard;

        if (liveBoard.getStops().length > 0) {
            this.lastSearchTime = liveBoard.getStops()[liveBoard.getStops().length - 1].getDepartureTime().plusMinutes(1);
        } else {
            this.lastSearchTime = liveBoard.getSearchTime().plusHours(1);
        }

        api.getLiveboard(liveBoard, lastSearchTime, RouteTimeDefinition.DEPART, this, this, TAG_APPEND);
    }

    public void prependLiveboard(final LiveBoard liveBoard, final IRailSuccessResponseListener<LiveBoard> successResponseListener,
                                 final IRailErrorResponseListener<LiveBoard> errorResponseListener) {
        this.successResponseListener = successResponseListener;
        this.errorResponseListener = errorResponseListener;

        this.originalLiveboard = liveBoard;

        if (liveBoard.getStops().length > 0) {
            this.lastSearchTime = liveBoard.getStops()[0].getDepartureTime();
        } else {
            this.lastSearchTime = liveBoard.getSearchTime();
        }

        api.getLiveboard(liveBoard, lastSearchTime, RouteTimeDefinition.ARRIVE, this, this, TAG_PREPEND);
    }

    @Override
    public void onSuccessResponse(LiveBoard data, Object tag) {
        switch ((int) tag) {
            case TAG_APPEND:
                if (data.getStops().length > 0) {
                    TrainStop[] mergedStops = ArrayUtils.concatenate(originalLiveboard.getStops(), data.getStops());
                    LiveBoard merged = new LiveBoard(originalLiveboard, mergedStops, originalLiveboard.getSearchTime());
                    this.successResponseListener.onSuccessResponse(merged, tag);
                } else {
                    attempt++;
                    lastSearchTime = lastSearchTime.plusHours(1);

                    if (attempt < 12) {
                        api.getLiveboard(originalLiveboard, lastSearchTime, RouteTimeDefinition.DEPART, this, this, tag);
                    } else {
                        if (this.successResponseListener != null) {
                            this.successResponseListener.onSuccessResponse(originalLiveboard, this);
                        }
                    }
                }
                break;
            case TAG_PREPEND:
                if (data.getStops().length > 0) {
                    TrainStop[] mergedStops = ArrayUtils.concatenate(data.getStops(), originalLiveboard.getStops());
                    LiveBoard merged = new LiveBoard(originalLiveboard, mergedStops, originalLiveboard.getSearchTime());
                    this.successResponseListener.onSuccessResponse(merged, tag);
                } else {
                    attempt++;
                    lastSearchTime = lastSearchTime.minusHours(1);

                    if (attempt < 12) {
                        api.getLiveboard(originalLiveboard, lastSearchTime, RouteTimeDefinition.ARRIVE, this, this, tag);
                    } else {
                        if (this.successResponseListener != null) {
                            this.successResponseListener.onSuccessResponse(originalLiveboard, this);
                        }
                    }
                }
                break;
        }
    }

    @Override
    public void onErrorResponse(Exception e, Object tag) {
        if (this.errorResponseListener != null) {
            this.errorResponseListener.onErrorResponse(e, this);
        }

    }
}
