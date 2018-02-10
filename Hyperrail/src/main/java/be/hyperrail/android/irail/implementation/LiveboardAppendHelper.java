/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.irail.implementation;

import android.support.annotation.NonNull;

import org.joda.time.DateTime;

import java.util.Arrays;

import be.hyperrail.android.irail.contracts.IRailErrorResponseListener;
import be.hyperrail.android.irail.contracts.IRailSuccessResponseListener;
import be.hyperrail.android.irail.contracts.IrailDataProvider;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.requests.IrailLiveboardRequest;
import be.hyperrail.android.util.ArrayUtils;

public class LiveboardAppendHelper implements IRailSuccessResponseListener<LiveBoard>, IRailErrorResponseListener {

    private final int TAG_APPEND = 0;
    private final int TAG_PREPEND = 1;

    private int attempt = 0;
    private DateTime lastSearchTime;
    private LiveBoard originalLiveboard;

    private IRailSuccessResponseListener<LiveBoard> successResponseListener;
    private IRailErrorResponseListener errorResponseListener;

    IrailDataProvider api = IrailFactory.getDataProviderInstance();

    public void appendLiveboard(@NonNull final LiveBoard liveBoard, final IRailSuccessResponseListener<LiveBoard> successResponseListener,
                                final IRailErrorResponseListener errorResponseListener) {

        this.successResponseListener = successResponseListener;
        this.errorResponseListener = errorResponseListener;

        this.originalLiveboard = liveBoard;

        if (liveBoard.getStops().length > 0) {
            this.lastSearchTime = liveBoard.getStops()[liveBoard.getStops().length - 1].getDepartureTime().plusMinutes(1);
        } else {
            this.lastSearchTime = liveBoard.getSearchTime().plusHours(1);
        }

        IrailLiveboardRequest request = new IrailLiveboardRequest(liveBoard,RouteTimeDefinition.DEPART,lastSearchTime);
        request.setCallback(this,this,TAG_APPEND);
        api.getLiveboard(request);
    }

    public void prependLiveboard(@NonNull final LiveBoard liveBoard, final IRailSuccessResponseListener<LiveBoard> successResponseListener,
                                 final IRailErrorResponseListener errorResponseListener) {
        this.successResponseListener = successResponseListener;
        this.errorResponseListener = errorResponseListener;

        this.originalLiveboard = liveBoard;

        if (liveBoard.getStops().length > 0) {
            this.lastSearchTime = liveBoard.getStops()[0].getDepartureTime();
        } else {
            this.lastSearchTime = liveBoard.getSearchTime();
        }
        IrailLiveboardRequest request = new IrailLiveboardRequest(liveBoard,RouteTimeDefinition.DEPART,lastSearchTime);
        request.setCallback(this,this,TAG_PREPEND);
        api.getLiveboardBefore(request);
    }

    @Override
    public void onSuccessResponse(@NonNull LiveBoard data, Object tag) {
        switch ((int) tag) {
            case TAG_APPEND:

                TrainStop[] newStops = data.getStops();

                if (newStops.length > 0) {
                    // It can happen that a scheduled departure was before the search time.
                    // In this case, prevent duplicates by searching the first stop which isn't before
                    // the searchdate, and removing all earlier stops.
                    int i = 0;
                    while (i < newStops.length && newStops[i].getDepartureTime().isBefore(data.getSearchTime())) {
                        i++;
                    }
                    if (i > 0) {
                        if (i <= data.getStops().length - 1) {
                            newStops = Arrays.copyOfRange(data.getStops(), i, data.getStops().length - 1);
                        } else {
                            newStops = new TrainStop[0];
                        }
                    }
                }

                if (newStops.length > 0) {

                    TrainStop[] mergedStops = ArrayUtils.concatenate(originalLiveboard.getStops(), newStops);
                    LiveBoard merged = new LiveBoard(originalLiveboard, mergedStops, originalLiveboard.getSearchTime(), originalLiveboard.getTimeDefinition());
                    this.successResponseListener.onSuccessResponse(merged, tag);
                } else {
                    // No results, search two hours further in case this day doesn't have results.
                    // Skip 2 hours at once, possible due to large API pages.
                    attempt++;
                    lastSearchTime = lastSearchTime.plusHours(2);

                    if (attempt < 12) {
                        IrailLiveboardRequest request = new IrailLiveboardRequest(originalLiveboard,RouteTimeDefinition.DEPART,lastSearchTime);
                        request.setCallback(this,this,TAG_APPEND);
                        api.getLiveboard(request);
                    } else {
                        if (this.successResponseListener != null) {
                            this.successResponseListener.onSuccessResponse(originalLiveboard, this);
                        }
                    }
                }
                break;
            case TAG_PREPEND:
                if (data.getStops().length > 0) {
                    // TODO: prevent duplicates by checking arrival time
                    TrainStop[] mergedStops = ArrayUtils.concatenate(data.getStops(), originalLiveboard.getStops());
                    LiveBoard merged = new LiveBoard(originalLiveboard, mergedStops, originalLiveboard.getSearchTime(), originalLiveboard.getTimeDefinition());
                    this.successResponseListener.onSuccessResponse(merged, tag);
                } else {
                    attempt++;
                    lastSearchTime = lastSearchTime.minusHours(1);

                    if (attempt < 12) {
                        IrailLiveboardRequest request = new IrailLiveboardRequest(originalLiveboard,RouteTimeDefinition.DEPART,lastSearchTime);
                        request.setCallback(this,this,TAG_PREPEND);
                        api.getLiveboardBefore(request);
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
    public void onErrorResponse(@NonNull Exception e, Object tag) {
        if (this.errorResponseListener != null) {
            this.errorResponseListener.onErrorResponse(e, this);
        }

    }
}
