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

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package eu.opentransport.lc2Irail;

import android.support.annotation.NonNull;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eu.opentransport.common.contracts.TransportOccupancyLevel;
import eu.opentransport.common.contracts.TransportStopsDataSource;
import eu.opentransport.common.exceptions.StopLocationNotResolvedException;
import eu.opentransport.common.models.LiveboardType;
import eu.opentransport.common.models.Route;
import eu.opentransport.common.models.RouteLeg;
import eu.opentransport.common.models.RouteLegEnd;
import eu.opentransport.common.models.RouteLegType;
import eu.opentransport.common.models.StopLocation;
import eu.opentransport.common.models.VehicleStopType;
import eu.opentransport.common.requests.LiveboardRequest;
import eu.opentransport.common.requests.RoutePlanningRequest;
import eu.opentransport.common.requests.VehicleRequest;
import eu.opentransport.irail.IrailLiveboard;
import eu.opentransport.irail.IrailRoute;
import eu.opentransport.irail.IrailRouteLeg;
import eu.opentransport.irail.IrailRouteLegEnd;
import eu.opentransport.irail.IrailRoutesList;
import eu.opentransport.irail.IrailVehicle;
import eu.opentransport.irail.IrailVehicleStop;
import eu.opentransport.irail.IrailVehicleStub;

/**
 * A simple parser for api.irail.be.
 *
 * @inheritDoc
 */
public class Lc2IrailParser {

    private final TransportStopsDataSource stationProvider;
    private DateTimeFormatter dtf = ISODateTimeFormat.dateTimeNoMillis();

    Lc2IrailParser(@NonNull TransportStopsDataSource stationProvider) {
        this.stationProvider = stationProvider;
    }

    @NonNull
    public IrailLiveboard parseLiveboard(@NonNull LiveboardRequest request, @NonNull JSONObject json) throws JSONException {
        List<IrailVehicleStop> stops = new ArrayList<>();
        JSONArray jsonStops = json.getJSONArray("stops");
        for (int i = 0; i < jsonStops.length(); i++) {
            stops.add(parseLiveboardStop(request, jsonStops.getJSONObject(i)));
        }

        JSONArray departuresOrArrivals;
        if (request.getType() == LiveboardType.DEPARTURES) {
            departuresOrArrivals = json.getJSONArray("departures");
        } else {
            departuresOrArrivals = json.getJSONArray("arrivals");
        }
        for (int i = 0; i < departuresOrArrivals.length(); i++) {
            stops.add(parseLiveboardStop(request, departuresOrArrivals.getJSONObject(i)));
        }

        IrailVehicleStop[] stopArray = new IrailVehicleStop[stops.size()];
        stops.toArray(stopArray);
        Arrays.sort(stopArray, (o1, o2) -> {
            if (o1.getDepartureTime() != null && o2.getDepartureTime() != null) {
                return o1.getDepartureTime().compareTo(o2.getDepartureTime());
            }
            if (o1.getArrivalTime() != null && o2.getArrivalTime() != null) {
                return o1.getArrivalTime().compareTo(o2.getArrivalTime());
            }
            if (o1.getDepartureTime() != null && o2.getArrivalTime() != null) {
                return o1.getDepartureTime().compareTo(o2.getArrivalTime());
            }
            return o1.getArrivalTime().compareTo(o2.getDepartureTime());
        });
        return new IrailLiveboard(request.getStation(), stopArray, request.getSearchTime(), request.getType(), request.getTimeDefinition());
    }

    @NonNull
    private IrailVehicleStop parseLiveboardStop(@NonNull LiveboardRequest request, @NonNull JSONObject json) throws JSONException {
        /*
        "arrivalDelay": 0,
          "arrivalTime": "2018-04-15T23:01:00+02:00",
          "departureDelay": 0,
          "departureTime": "2018-04-15T23:06:00+02:00",
          "hasArrived": false,
          "hasDeparted": false,
          "isArrivalCanceled": false,
          "isDepartureCanceled": false,
          "platform": "?",
          "uri": "http://irail.be/connections/8841004/20180415/IC545",
          "vehicle": {
            "uri": "http://irail.be/vehicle/IC545/20180415",
            "id": "IC545",
            "direction": "Ostende"
          }
         */

        int departureDelay = 0;
        int arrivalDelay = 0;
        DateTime departureTime = null;
        DateTime arrivalTime = null;
        String platform = "?";
        String uri = null;
        IrailVehicleStub vehicle = null;
        boolean hasDeparted = false;
        boolean hasArrived = false;

        if (json.has("arrivalTime")) {
            arrivalTime = DateTime.parse(json.getString("arrivalTime"), dtf);
            arrivalDelay = json.getInt("arrivalDelay");
        }
        if (json.has("departureTime")) {
            departureTime = DateTime.parse(json.getString("departureTime"), dtf);
            departureDelay = json.getInt("departureDelay");
        }

        if (json.has("hasDeparted")) {
            hasDeparted = json.getBoolean("hasDeparted");
        }
        if (json.has("hasArrived")) {
            hasArrived = json.getBoolean("hasArrived");
        }

        platform = json.getString("platform");

        uri = json.getString("uri");

        String headsign = json.getJSONObject("vehicle").getString("direction");
        StopLocation headsignStation = stationProvider.getStationByExactName(headsign);
        if (headsignStation != null) {
            headsign = headsignStation.getLocalizedName();
        }

        vehicle = new IrailVehicleStub(
                json.getJSONObject("vehicle").getString("id"),
                headsign,
                json.getJSONObject("vehicle").getString("uri")
        );


        VehicleStopType type;
        if (departureTime != null) {
            if (arrivalTime != null) {
                type = VehicleStopType.STOP;
            } else {
                type = VehicleStopType.DEPARTURE;
            }
        } else {
            if (arrivalTime != null) {
                type = VehicleStopType.ARRIVAL;
            } else {
                throw new IllegalStateException("Departure time or arrival time is required!");
            }
        }

        boolean departureCanceled = false;
        if (json.has("isDepartureCanceled")) {
            departureCanceled = json.getBoolean("isDepartureCanceled");
        }

        boolean arrivalCanceled = false;
        if (json.has("isArrivalCanceled")) {
            arrivalCanceled = json.getBoolean("isArrivalCanceled");
        }

        return new IrailVehicleStop(request.getStation(),
                                    vehicle,
                                    platform,
                                    true,
                                    departureTime, arrivalTime, Duration.standardSeconds(departureDelay),
                                    Duration.standardSeconds(arrivalDelay),
                                    departureCanceled,
                                    arrivalCanceled,
                                    hasDeparted,
                                    uri,
                                    TransportOccupancyLevel.UNSUPPORTED,
                                    type
        );

    }

    @NonNull
    public IrailVehicle parseVehicle(@NonNull VehicleRequest request, @NonNull JSONObject response) throws JSONException, StopLocationNotResolvedException {
        String id = response.getString("id");
        String uri = response.getString("uri");

        IrailVehicleStub vehicleStub = new IrailVehicleStub(id, response.getString("direction"), uri);
        JSONArray jsonStops = response.getJSONArray("stops");
        IrailVehicleStop stops[] = new IrailVehicleStop[jsonStops.length()];

        double latitude = 0;
        double longitude = 0;

        for (int i = 0; i < jsonStops.length(); i++) {
            VehicleStopType type = VehicleStopType.STOP;
            if (i == 0) {
                type = VehicleStopType.DEPARTURE;
            } else if (i == jsonStops.length() - 1) {
                type = VehicleStopType.ARRIVAL;
            }

            stops[i] = parseVehicleStop(request, jsonStops.getJSONObject(i), vehicleStub, type);

            if (i == 0 || stops[i].hasLeft()) {
                longitude = stops[i].getStation().getLongitude();
                latitude = stops[i].getStation().getLatitude();
            }
        }
        return new IrailVehicle(id, uri, longitude, latitude, stops);
    }

    @NonNull
    private IrailVehicleStop parseVehicleStop(@NonNull VehicleRequest request, @NonNull JSONObject json, @NonNull IrailVehicleStub vehicle, @NonNull VehicleStopType type) throws JSONException, StopLocationNotResolvedException {
        /*
        {
              "arrivalDelay": 0,
              "arrivalTime": "2018-04-13T15:25:00+02:00",
              "departureDelay": 0,
              "departureTime": "2018-04-13T15:26:00+02:00",
              "hasArrived": true,
              "hasDeparted": true,
              "isArrivalCanceled": false,
              "isDepartureCanceled": false,
              "platform": "?",
              "station": {
                "hid": "008844503",
                "uicCode": "8844503",
                "uri": "http://irail.be/stations/NMBS/008844503",
                "defaultName": "Welkenraedt",
                "localizedName": "Welkenraedt",
                "latitude": "50.659707",
                "longitude": "5.975381",
                "countryCode": "be",
                "countryURI": "http://sws.geonames.org/2802361/"
              },
              "uri": "http://irail.be/connections/8844503/20180413/IC538"
        },
        */

        int departureDelay = 0;
        int arrivalDelay = 0;
        DateTime departureTime = null;
        DateTime arrivalTime = null;
        String platform = "?";
        String uri = null;
        boolean hasArrived = false;
        boolean hasDeparted = false;
        boolean isDepartureCanceled = false;
        boolean isArrivalCanceled = false;
        // TODO: parse isPlatformNormal from response
        boolean isPlatformNormal = true;

        StopLocation station = stationProvider.getStationByUri(json.getJSONObject("station").getString("uri"));

        if (json.has("arrivalTime")) {
            arrivalTime = DateTime.parse(json.getString("arrivalTime"), dtf);
            arrivalDelay = json.getInt("arrivalDelay");
        }

        if (json.has("departureTime")) {
            departureTime = DateTime.parse(json.getString("departureTime"), dtf);
            departureDelay = json.getInt("departureDelay");
        }

        if (json.has("platform")) {
            platform = json.getString("platform");
        }

        if (json.has("hasDeparted")) {
            hasDeparted = json.getBoolean("hasDeparted");
        }

        if (json.has("hasArrived")) {
            hasArrived = json.getBoolean("hasArrived");
        }

        if (json.has("isDepartureCanceled")) {
            isDepartureCanceled = json.getBoolean("isDepartureCanceled");
        }

        if (json.has("isArrivalCanceled")) {
            isArrivalCanceled = json.getBoolean("isArrivalCanceled");
        }

        if (json.has("uri")) {
            uri = json.getString("uri");
        }

        return new IrailVehicleStop(station,
                                    vehicle,
                                    platform,
                                    isPlatformNormal,
                                    departureTime, arrivalTime, Duration.standardSeconds(departureDelay),
                                    Duration.standardSeconds(arrivalDelay),
                                    isDepartureCanceled,
                                    isArrivalCanceled,
                                    hasDeparted,
                                    uri,
                                    TransportOccupancyLevel.UNSUPPORTED,
                                    type
        );
    }

    @NonNull
    public IrailRoutesList parseRoutes(@NonNull RoutePlanningRequest request, @NonNull JSONObject json) throws JSONException, StopLocationNotResolvedException {
        StopLocation origin = stationProvider.getStationByUri(json.getJSONObject("departureStation").getString("uri"));
        StopLocation destination = stationProvider.getStationByUri(json.getJSONObject("arrivalStation").getString("uri"));

        JSONArray connections = json.getJSONArray("connections");
        Route[] routes = new Route[connections.length()];
        for (int i = 0; i < connections.length(); i++) {
            routes[i] = parseConnection(request, connections.getJSONObject(i));
        }

        return new IrailRoutesList(origin, destination, request.getSearchTime(), request.getTimeDefinition(), routes);
    }

    /**
     * Parse a connection existing of one or more legs
     *
     * @param request The request for routes used to become this response
     * @param json    The connection JSON object
     * @return The object representation of the passed JSON
     */
    @NonNull
    private Route parseConnection(@NonNull RoutePlanningRequest request, @NonNull JSONObject json) throws JSONException, StopLocationNotResolvedException {
        /*
          "legs": [
                {
                  "arrivalDelay": 0,
                  "arrivalPlatform": "?",
                  "arrivalStation": {
                    "hid": "008814001",
                    "uicCode": "8814001",
                    "uri": "http://irail.be/stations/NMBS/008814001",
                    "defaultName": "Brussel-Zuid/Bruxelles-Midi",
                    "localizedName": "Brussels-South/Brussels-Midi",
                    "latitude": "50.835707",
                    "longitude": "4.336531",
                    "countryCode": "be",
                    "countryURI": "http://sws.geonames.org/2802361/"
                  },
                  "arrivalTime": "2018-04-15T18:00:00+00:00",
                  "arrivalUri": "http://irail.be/connections/8813003/20180415/IC541",
                  "departureDelay": 0,
                  "departurePlatform": "?",
                  "departureStation": {
                    "hid": "008841004",
                    "uicCode": "8841004",
                    "uri": "http://irail.be/stations/NMBS/008841004",
                    "defaultName": "Liège-Guillemins",
                    "localizedName": "Liège-Guillemins",
                    "latitude": "50.62455",
                    "longitude": "5.566695",
                    "countryCode": "be",
                    "countryURI": "http://sws.geonames.org/2802361/"
                  },
                  "departureTime": "2018-04-15T17:01:00+00:00",
                  "departureUri": "http://irail.be/connections/8841004/20180415/IC541",
                  "direction": "Ostende",
                  "hasArrived": true,
                  "hasLeft": true,
                  "isArrivalCanceled": false,
                  "isArrivalPlatformNormal": true,
                  "isDepartureCanceled": false,
                  "isDeparturePlatformNormal": true,
                  "route": "IC541",
                  "trip": "http://irail.be/vehicle/IC541/20180415"
                }
              ],
              "departureTime": "2018-04-15T17:01:00+00:00",
              "arrivalTime": "2018-04-15T18:00:00+00:00"
            },
         */
        JSONArray jsonlegs = json.getJSONArray("legs");
        RouteLeg[] legs = new RouteLeg[jsonlegs.length()];

        for (int i = 0; i < jsonlegs.length(); i++) {
            JSONObject jsonLeg = jsonlegs.getJSONObject(i);

            String headsign = jsonLeg.getString("direction");
            StopLocation headsignStation = stationProvider.getStationByExactName(headsign);
            if (headsignStation != null) {
                headsign = headsignStation.getLocalizedName();
            }

            IrailVehicleStub vehicle = new IrailVehicleStub(
                    jsonLeg.getString("route"),
                    headsign,
                    jsonLeg.getString("trip")
            );

            StopLocation departureStation = stationProvider.getStationByUri(jsonLeg.getJSONObject("departureStation").getString("uri"));
            StopLocation arrivalStation = stationProvider.getStationByUri(jsonLeg.getJSONObject("arrivalStation").getString("uri"));

            DateTime departureTime = DateTime.parse(jsonLeg.getString("departureTime"), dtf);
            int departureDelay = jsonLeg.getInt("departureDelay");

            boolean isDepartureCanceled = false;
            if (jsonLeg.has("isDepartureCanceled")) {
                isDepartureCanceled = jsonLeg.getBoolean("isDepartureCanceled");
            }
            boolean isDeparturePlatformNormal = true;
            if (jsonLeg.has("isDeparturePlatformNormal")) {
                isDeparturePlatformNormal = jsonLeg.getBoolean("isDeparturePlatformNormal");
            }
            boolean hasDeparted = false;
            if (jsonLeg.has("hasLeft")) {
                hasDeparted = jsonLeg.getBoolean("hasLeft");
            }

            RouteLegEnd departure = new IrailRouteLegEnd(departureStation,
                                                    departureTime,
                                                    jsonLeg.getString("departurePlatform"),
                                                    isDeparturePlatformNormal,
                                                    Duration.standardSeconds(departureDelay),
                                                    isDepartureCanceled,
                                                    hasDeparted,
                                                    jsonLeg.getString("departureUri"),
                                                    TransportOccupancyLevel.UNSUPPORTED);

            boolean isArrivalCanceled = false;
            if (jsonLeg.has("isArrivalCanceled")) {
                isArrivalCanceled = jsonLeg.getBoolean("isArrivalCanceled");
            }
            boolean isArrivalPlatformNormal = true;
            if (jsonLeg.has("isArrivalPlatformNormal")) {
                isArrivalPlatformNormal = jsonLeg.getBoolean("isArrivalPlatformNormal");
            }
            boolean hasArrived = false;
            if (jsonLeg.has("hasArrived")) {
                hasArrived = jsonLeg.getBoolean("hasArrived");
            }
            DateTime arrivalTime = DateTime.parse(jsonLeg.getString("arrivalTime"), dtf);
            int arrivalDelay = jsonLeg.getInt("arrivalDelay");

            RouteLegEnd arrival = new IrailRouteLegEnd(arrivalStation,
                                                       arrivalTime,
                                                       jsonLeg.getString("arrivalPlatform"),
                                                       isArrivalPlatformNormal,
                                                       Duration.standardSeconds(arrivalDelay),
                                                       isArrivalCanceled,
                                                       hasArrived,
                                                       jsonLeg.getString("arrivalUri"),
                                                       TransportOccupancyLevel.UNSUPPORTED);

            legs[i] = new IrailRouteLeg(RouteLegType.TRAIN, vehicle, departure, arrival);
        }
        return new IrailRoute(legs);
    }
}
