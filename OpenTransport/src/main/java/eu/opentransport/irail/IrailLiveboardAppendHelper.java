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

package eu.opentransport.irail;

import org.joda.time.DateTime;

import java.util.Arrays;

import eu.opentransport.OpenTransportApi;
import eu.opentransport.common.contracts.QueryTimeDefinition;
import eu.opentransport.common.contracts.TransportDataErrorResponseListener;
import eu.opentransport.common.contracts.TransportDataSource;
import eu.opentransport.common.contracts.TransportDataSuccessResponseListener;
import eu.opentransport.common.models.Liveboard;
import eu.opentransport.common.models.LiveboardType;
import eu.opentransport.common.models.VehicleStop;
import eu.opentransport.common.models.VehicleStopType;
import eu.opentransport.common.requests.ExtendLiveboardRequest;
import eu.opentransport.common.requests.LiveboardRequest;

/**
 * A class which allows to withStopsAppended liveboards.
 */
public class IrailLiveboardAppendHelper implements TransportDataSuccessResponseListener<Liveboard>, TransportDataErrorResponseListener {

    private final int TAG_APPEND = 0;
    private final int TAG_PREPEND = 1;

    private int attempt = 0;
    private DateTime lastSearchTime;
    private IrailLiveboard originalLiveboard;
    private ExtendLiveboardRequest mExtendRequest;

    TransportDataSource api = OpenTransportApi.getDataProviderInstance();

    public void extendLiveboard(ExtendLiveboardRequest extendRequest) {
        switch (extendRequest.getAction()) {
            default:
            case APPEND:
                appendLiveboard(extendRequest);
                break;
            case PREPEND:
                prependLiveboard(extendRequest);
                break;
        }
    }

    private void appendLiveboard(ExtendLiveboardRequest extendRequest) {

        this.originalLiveboard = (IrailLiveboard) extendRequest.getLiveboard();
        mExtendRequest = extendRequest;

        if (originalLiveboard.getStops().length > 0) {
            if (originalLiveboard.getStops()[originalLiveboard.getStops().length - 1].getType() == VehicleStopType.DEPARTURE) {
                this.lastSearchTime = originalLiveboard.getStops()[originalLiveboard.getStops().length - 1].getDepartureTime();
            } else {
                this.lastSearchTime = originalLiveboard.getStops()[originalLiveboard.getStops().length - 1].getArrivalTime();
            }
        } else {
            this.lastSearchTime = originalLiveboard.getSearchTime().plusHours(1);
        }

        LiveboardRequest request = new LiveboardRequest(originalLiveboard, QueryTimeDefinition.DEPART_AT, originalLiveboard.getLiveboardType(), lastSearchTime);
        request.setCallback(this, this, TAG_APPEND);
        api.getLiveboard(request);
    }

    private void prependLiveboard(ExtendLiveboardRequest extendRequest) {
        this.originalLiveboard = (IrailLiveboard) extendRequest.getLiveboard();
        mExtendRequest = extendRequest;

        if (originalLiveboard.getStops().length > 0) {
            if (originalLiveboard.getStops()[originalLiveboard.getStops().length - 1].getType() == VehicleStopType.DEPARTURE) {
                this.lastSearchTime = originalLiveboard.getStops()[0].getDepartureTime().minusHours(1);
            } else {
                this.lastSearchTime = originalLiveboard.getStops()[0].getArrivalTime().minusHours(1);
            }
        } else {
            this.lastSearchTime = originalLiveboard.getSearchTime().minusHours(1);
        }
        LiveboardRequest request = new LiveboardRequest(originalLiveboard, QueryTimeDefinition.ARRIVE_AT, originalLiveboard.getLiveboardType(), lastSearchTime);
        request.setCallback(this, this, TAG_PREPEND);
        api.getLiveboard(request);
    }

    @Override
    public void onSuccessResponse(Liveboard data, Object tag) {
        switch ((int) tag) {
            case TAG_APPEND:
                handleAppendSuccessResponse((IrailLiveboard) data);
                break;
            case TAG_PREPEND:
                handlePrependSuccessResponse((IrailLiveboard) data);
                break;
        }
    }

    /**
     * Handle a successful response when prepending a liveboard
     *
     * @param data The newly received data
     */
    private void handlePrependSuccessResponse(IrailLiveboard data) {
        // If there is new data
        if (data.getStops().length > 0) {
            mExtendRequest.notifySuccessListeners(originalLiveboard.withStopsAppended(data));
        } else {
            attempt++;
            lastSearchTime = lastSearchTime.minusHours(1);

            if (attempt < 12) {
                LiveboardRequest request = new LiveboardRequest(originalLiveboard, QueryTimeDefinition.ARRIVE_AT, originalLiveboard.getLiveboardType(), lastSearchTime);
                request.setCallback(this, this, TAG_PREPEND);
                api.getLiveboard(request);
            } else {
                mExtendRequest.notifySuccessListeners(originalLiveboard);
            }
        }
    }

    /**
     * Handle a successful response when appending a liveboard
     *
     * @param data The newly received data
     */
    private void handleAppendSuccessResponse(IrailLiveboard data) {
        VehicleStop[] newStops = data.getStops();

        if (newStops.length > 0) {
            // It can happen that a scheduled departure was before the search time.
            // In this case, prevent duplicates by searching the first stop which isn't before
            // the searchdate, and removing all earlier stops.
            int i = 0;

            if (data.getLiveboardType() == LiveboardType.DEPARTURES) {
                while (i < newStops.length && newStops[i].getDepartureTime().isBefore(data.getSearchTime())) {
                    i++;
                }
            } else {
                while (i < newStops.length && newStops[i].getArrivalTime().isBefore(data.getSearchTime())) {
                    i++;
                }
            }

            if (i > 0) {
                if (i <= data.getStops().length - 1) {
                    newStops = Arrays.copyOfRange(data.getStops(), i, data.getStops().length - 1);
                } else {
                    newStops = new IrailVehicleStop[0];
                }
            }
        }

        if (newStops.length > 0) {
            mExtendRequest.notifySuccessListeners(originalLiveboard.withStopsAppended(data));
        } else {
            // No results, search two hours further in case this day doesn't have results.
            // Skip 2 hours at once, possible due to large API pages.
            attempt++;
            lastSearchTime = lastSearchTime.plusHours(2);

            if (attempt < 12) {
                LiveboardRequest request = new LiveboardRequest(originalLiveboard, QueryTimeDefinition.DEPART_AT, originalLiveboard.getLiveboardType(), lastSearchTime);
                request.setCallback(this, this, TAG_APPEND);
                api.getLiveboard(request);
            } else {
                mExtendRequest.notifySuccessListeners(originalLiveboard);
            }
        }
    }

    @Override
    public void onErrorResponse(Exception e, Object tag) {
        mExtendRequest.notifyErrorListeners(e);
    }
}
