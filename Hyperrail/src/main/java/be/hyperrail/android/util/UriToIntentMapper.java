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

import android.content.Intent;
import android.location.Location;
import android.net.Uri;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.HashMap;
import java.util.Map;

import be.hyperrail.android.activities.LinkDispatcherActivity;
import be.hyperrail.android.activities.searchresult.LiveboardActivity;
import be.hyperrail.android.activities.searchresult.RouteActivity;
import be.hyperrail.android.irail.contracts.IrailStationProvider;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.db.Station;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.Liveboard;
import be.hyperrail.android.irail.implementation.requests.IrailLiveboardRequest;
import be.hyperrail.android.irail.implementation.requests.IrailRoutesRequest;

/**
 * Created in be.hyperrail.android.activities on 12/04/2018.
 */
public class UriToIntentMapper {
    private final LinkDispatcherActivity mContext;

    public UriToIntentMapper(LinkDispatcherActivity context) {
        mContext = context;
    }

    public void dispatchIntent(Intent intent) {
        Uri uri = intent.getData();
        Intent dispatchIntent = null;

        if (uri == null) throw new IllegalArgumentException("Uri cannot be null");

        String scheme = uri.getScheme().toLowerCase();
        String host = uri.getHost().toLowerCase();

        if ((scheme.equals("http") || scheme.equals("https")) &&
                (host.equals("belgianrail.be") || host.equals("www.belgianrail.be"))) {
            dispatchIntent = mapNmbsLink(uri);
        }

        if (dispatchIntent != null) {
            mContext.startActivity(dispatchIntent);
        }
    }

    private Intent mapNmbsLink(Uri uri) {
        String path = uri.getPath();
        IrailStationProvider stationProvider = IrailFactory.getStationsProviderInstance();
        DateTimeFormatter dtf = DateTimeFormat.forPattern("HH:mm dd/MM/yyyy");

        Station departureStation = null;
        Station arrivalStation = null;

        Map<String, String> departureParameters = getJourneyStopParameter(uri.getQueryParameter("REQ0JourneyStopsSID"));
        Map<String, String> arrivalParameters = getJourneyStopParameter(uri.getQueryParameter("REQ0JourneyStopsSID"));

        if (departureParameters != null && departureParameters.containsKey("L")) {
            try {
                departureStation = stationProvider.getStationByHID(departureParameters.get("L"), true);

                Location targetLocation = new Location("");//provider name is unnecessary
                targetLocation.setLatitude(Double.parseDouble(departureParameters.get("Y")) / 1000000);
                targetLocation.setLongitude(Double.parseDouble(departureParameters.get("X")) / 1000000);
                departureStation = stationProvider.getStationsOrderByLocation(targetLocation)[0];
            } catch (Exception e) {
                // Ignored as we know this variable isn't always present
            }
        }

        if (arrivalParameters != null && arrivalParameters.containsKey("L")) {
            try {
                arrivalStation = stationProvider.getStationByHID(arrivalParameters.get("L"), true);
                Location targetLocation = new Location("");//provider name is unnecessary
                targetLocation.setLatitude(Double.parseDouble(arrivalParameters.get("Y")) / 1000000);
                targetLocation.setLongitude(Double.parseDouble(arrivalParameters.get("X")) / 1000000);
                arrivalStation = stationProvider.getStationsOrderByLocation(targetLocation)[0];
            } catch (Exception e) {
                // Ignored as we know this variable isn't always present
            }
        }


        switch (path) {
            case "/jp/sncb-routeplanner/query.exe/nn":
            case "/jp/nmbs-routeplanner/query.exe/nn":
            case "/jp/sncb-nmbs-routeplanner/query.exe/nn":
                // Route:
                // http://www.belgianrail.be/jp/nmbs-routeplanner/query.exe/nn?S=Halle&Z=Brussel&date=12/04/2018&time=20:30&start=1&timesel=depart&&REQ0JourneyStopsSID=A=1@O=Halle@X=4240634@Y=50733931@U=80@L=008814308@B=1@p=1523491001@n=ac.1=GA@&REQ0JourneyStopsZID=A=1@O=Brussel@X=4356802@Y=50845658@U=80@L=008813003@B=1@p=1523491001@n=ac.1=GA@&REQ0JourneyProduct_prod_list=3:0111111111111111&OK#focus
                if (departureStation == null) {
                    departureStation = stationProvider.getStationByName(uri.getQueryParameter("S"));
                }

                if (arrivalStation == null) {
                    arrivalStation = stationProvider.getStationByName(uri.getQueryParameter("Z"));
                }

                IrailRoutesRequest routesRequest = new IrailRoutesRequest(departureStation, arrivalStation,
                                                                          uri.getQueryParameter("timesel").equals("depart") ? RouteTimeDefinition.DEPART_AT : RouteTimeDefinition.ARRIVE_AT,
                                                                          DateTime.parse(uri.getQueryParameter("time") + " " + uri.getQueryParameter("date"), dtf));

                return RouteActivity.createIntent(mContext, routesRequest);
            case "/jp/sncb-routeplanner/stboard.exe/nn":
            case "/jp/nmbs-routeplanner/stboard.exe/nn":
            case "/jp/sncb-nmbs-routeplanner/stboard.exe/nn":
                // http://www.belgianrail.be/jp/sncb-nmbs-routeplanner/stboard.exe/nn?HWAI=VIEW!view=realtimeinfo!&productsFilter=01111110000&maxJourneys=50&input=Halle&date=12/04/2018
                // &time=21:04&boardType=dep&start=1&REQ0JourneyStopsSID=A=1@O=Halle@X=4240634@Y=50733931@U=80@L=008814308@B=1@p=1523491001@n=ac.1=GA@

                if (departureStation == null) {
                    departureStation = stationProvider.getStationByName(uri.getQueryParameter("input"));
                }

                IrailLiveboardRequest liveboardRequest = new IrailLiveboardRequest(departureStation,
                                                                                   RouteTimeDefinition.DEPART_AT,
                                                                                   Liveboard.LiveboardType.DEPARTURES,
                                                                                   DateTime.parse(uri.getQueryParameter("time") + " " + uri.getQueryParameter("date"), dtf));
                return LiveboardActivity.createIntent(mContext, liveboardRequest);

            case "/jp/sncb-nmbs-routeplanner/trainsearch.exe/nn":
                // http://www.belgianrail.be/jp/sncb-nmbs-routeplanner/trainsearch.exe/nn?ld=std&AjaxMap=CPTVMap&seqnr=1&ident=5g.030211113.1523607187&
                throw new IllegalArgumentException("Train search URI's for NMBS are not support");

        }
        throw new IllegalArgumentException("Invalid NMBS URI");
    }

    private static Map<String, String> getJourneyStopParameter(String parameter) {
        if (parameter == null || parameter.isEmpty()) {
            return null;
        }

        Map<String, String> result = new HashMap<>();
        String[] pieces = parameter.split("@");
        for (String piece : pieces) {
            String[] keyvalue = piece.split("=");
            result.put(keyvalue[0], keyvalue[1]);
        }
        return result;
    }
}