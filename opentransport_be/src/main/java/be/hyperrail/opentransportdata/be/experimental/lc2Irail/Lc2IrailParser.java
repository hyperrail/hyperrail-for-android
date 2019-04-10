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

package be.hyperrail.opentransportdata.be.experimental.lc2Irail;

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

import be.hyperrail.opentransportdata.be.irail.IrailVehicleJourneyStub;
import be.hyperrail.opentransportdata.common.contracts.TransportOccupancyLevel;
import be.hyperrail.opentransportdata.common.contracts.TransportStopsDataSource;
import be.hyperrail.opentransportdata.common.exceptions.StopLocationNotResolvedException;
import be.hyperrail.opentransportdata.common.models.LiveboardType;
import be.hyperrail.opentransportdata.common.models.Route;
import be.hyperrail.opentransportdata.common.models.RouteLeg;
import be.hyperrail.opentransportdata.common.models.RouteLegEnd;
import be.hyperrail.opentransportdata.common.models.RouteLegType;
import be.hyperrail.opentransportdata.common.models.StopLocation;
import be.hyperrail.opentransportdata.common.models.VehicleStopType;
import be.hyperrail.opentransportdata.common.requests.LiveboardRequest;
import be.hyperrail.opentransportdata.common.requests.RoutePlanningRequest;
import be.hyperrail.opentransportdata.common.requests.VehicleRequest;
import be.hyperrail.opentransportdata.common.models.implementation.LiveboardImpl;
import be.hyperrail.opentransportdata.common.models.implementation.RouteImpl;
import be.hyperrail.opentransportdata.common.models.implementation.RouteLegImpl;
import be.hyperrail.opentransportdata.common.models.implementation.RouteLegEndImpl;
import be.hyperrail.opentransportdata.common.models.implementation.RoutesListImpl;
import be.hyperrail.opentransportdata.be.irail.IrailVehicleJourney;
import be.hyperrail.opentransportdata.common.models.implementation.VehicleStopImpl;

/**
 * A simple parser for api.irail.be.
 *
 * @inheritDoc
 */
class Lc2IrailParser {

    private final TransportStopsDataSource stationProvider;
    private DateTimeFormatter dtf = ISODateTimeFormat.dateTimeNoMillis();

    Lc2IrailParser(@NonNull TransportStopsDataSource stationProvider) {
        this.stationProvider = stationProvider;
    }

    @NonNull
    LiveboardImpl parseLiveboard(@NonNull LiveboardRequest request, @NonNull JSONObject json) throws JSONException {
        List<VehicleStopImpl> stops = new ArrayList<>();
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

        VehicleStopImpl[] stopArray = new VehicleStopImpl[stops.size()];
        stops.toArray(stopArray);
        Arrays.sort(stopArray, this::compareLiveboardEntriesByTime);
        return new LiveboardImpl(request.getStation(), stopArray, request.getSearchTime(), request.getType(), request.getTimeDefinition());
    }

    private int compareLiveboardEntriesByTime(VehicleStopImpl o1, VehicleStopImpl o2) {
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
    }

    @NonNull
    private VehicleStopImpl parseLiveboardStop(@NonNull LiveboardRequest request, @NonNull JSONObject json) throws JSONException {
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
          "semanticId": "http://irail.be/connections/8841004/20180415/IC545",
          "vehicle": {
            "semanticId": "http://irail.be/vehicle/IC545/20180415",
            "id": "IC545",
            "direction": "Ostende"
          }
         */

        int departureDelay = 0;
        int arrivalDelay = 0;
        DateTime departureTime = null;
        DateTime arrivalTime = null;
        String platform;
        String uri;
        IrailVehicleJourneyStub vehicle;
        boolean hasDeparted;
        boolean hasArrived = false;

        if (json.has("arrivalTime")) {
            arrivalTime = DateTime.parse(json.getString("arrivalTime"), dtf);
            arrivalDelay = json.getInt("arrivalDelay");
        }
        if (json.has("departureTime")) {
            departureTime = DateTime.parse(json.getString("departureTime"), dtf);
            departureDelay = json.getInt("departureDelay");
        }

        hasDeparted = parseBooleanIfPresent(json, false, "hasDeparted");
        hasArrived = parseBooleanIfPresent(json, false, "hasArrived");

        platform = json.getString("platform");

        uri = json.getString("semanticId");

        String headsign = parseHeadsign(json.getJSONObject("vehicle"));

        vehicle = new IrailVehicleJourneyStub(
                json.getJSONObject("vehicle").getString("id"),
                headsign,
                json.getJSONObject("vehicle").getString("semanticId")
        );


        VehicleStopType type = parseVehicleStopType(departureTime, arrivalTime);

        boolean departureCanceled;
        departureCanceled = parseBooleanIfPresent(json, false, "isDepartureCanceled");

        boolean arrivalCanceled;
        arrivalCanceled = parseBooleanIfPresent(json, false, "isArrivalCanceled");

        return new VehicleStopImpl(request.getStation(),
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

    private String parseHeadsign(JSONObject vehicle2) throws JSONException {
        String headsign = vehicle2.getString("direction");
        StopLocation headsignStation = stationProvider.getStoplocationByExactName(headsign);
        if (headsignStation != null) {
            headsign = headsignStation.getLocalizedName();
        }
        return headsign;
    }

    @NonNull
    private VehicleStopType parseVehicleStopType(DateTime departureTime, DateTime arrivalTime) {
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
        return type;
    }

    @NonNull
    IrailVehicleJourney parseVehicleJourney(@NonNull VehicleRequest request, @NonNull JSONObject response) throws JSONException, StopLocationNotResolvedException {
        String id = response.getString("id");
        String uri = response.getString("semanticId");

        IrailVehicleJourneyStub vehicleStub = new IrailVehicleJourneyStub(id, response.getString("direction"), uri);
        JSONArray jsonStops = response.getJSONArray("stops");
        VehicleStopImpl stops[] = new VehicleStopImpl[jsonStops.length()];

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
                longitude = stops[i].getStopLocation().getLongitude();
                latitude = stops[i].getStopLocation().getLatitude();
            }
        }
        return new IrailVehicleJourney(id, uri, longitude, latitude, stops);
    }

    @NonNull
    private VehicleStopImpl parseVehicleStop(@NonNull VehicleRequest request, @NonNull JSONObject json, @NonNull IrailVehicleJourneyStub vehicle, @NonNull VehicleStopType type) throws JSONException, StopLocationNotResolvedException {
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
                "semanticId": "http://irail.be/stations/NMBS/008844503",
                "defaultName": "Welkenraedt",
                "localizedName": "Welkenraedt",
                "latitude": "50.659707",
                "longitude": "5.975381",
                "countryCode": "be",
                "countryURI": "http://sws.geonames.org/2802361/"
              },
              "semanticId": "http://irail.be/connections/8844503/20180413/IC538"
        },
        */

        int departureDelay = 0;
        int arrivalDelay = 0;
        DateTime departureTime = null;
        DateTime arrivalTime = null;
        String platform = "?";
        String uri = null;

        StopLocation station = stationProvider.getStoplocationBySemanticId(json.getJSONObject("station").getString("semanticId"));

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

        boolean hasDeparted = parseBooleanIfPresent(json, false, "hasDeparted");

        boolean  isPlatformNormal = parseBooleanIfPresent(json, true, "isPlatformNormal");

        boolean  hasArrived = parseBooleanIfPresent(json, false, "hasArrived");

        boolean   isDepartureCanceled = parseBooleanIfPresent(json, false, "isDepartureCanceled");

        boolean   isArrivalCanceled = parseBooleanIfPresent(json, false, "isArrivalCanceled");

        if (json.has("semanticId")) {
            uri = json.getString("semanticId");
        }

        return new VehicleStopImpl(station,
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
    RoutesListImpl parseRoutes(@NonNull RoutePlanningRequest request, @NonNull JSONObject json) throws JSONException, StopLocationNotResolvedException {
        StopLocation origin = stationProvider.getStoplocationBySemanticId(json.getJSONObject("departureStation").getString("semanticId"));
        StopLocation destination = stationProvider.getStoplocationBySemanticId(json.getJSONObject("arrivalStation").getString("semanticId"));

        JSONArray connections = json.getJSONArray("connections");
        Route[] routes = new Route[connections.length()];
        for (int i = 0; i < connections.length(); i++) {
            routes[i] = parseConnection(request, connections.getJSONObject(i));
        }

        return new RoutesListImpl(origin, destination, request.getSearchTime(), request.getTimeDefinition(), routes);
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
                    "semanticId": "http://irail.be/stations/NMBS/008814001",
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
                    "semanticId": "http://irail.be/stations/NMBS/008841004",
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

            String headsign = parseHeadsign(jsonLeg);

            IrailVehicleJourneyStub vehicle = new IrailVehicleJourneyStub(
                    jsonLeg.getString("route"),
                    headsign,
                    jsonLeg.getString("trip")
            );

            StopLocation departureStation = stationProvider.getStoplocationBySemanticId(jsonLeg.getJSONObject("departureStation").getString("semanticId"));
            StopLocation arrivalStation = stationProvider.getStoplocationBySemanticId(jsonLeg.getJSONObject("arrivalStation").getString("semanticId"));

            DateTime departureTime = DateTime.parse(jsonLeg.getString("departureTime"), dtf);
            int departureDelay = jsonLeg.getInt("departureDelay");

            boolean isDepartureCanceled = parseBooleanIfPresent(jsonLeg, false, "isDepartureCanceled");
            boolean isDeparturePlatformNormal = parseBooleanIfPresent(jsonLeg, true, "isDeparturePlatformNormal");
            boolean hasDeparted = parseBooleanIfPresent(jsonLeg, false, "hasLeft");

            RouteLegEnd departure = new RouteLegEndImpl(departureStation,
                                                    departureTime,
                                                    jsonLeg.getString("departurePlatform"),
                                                    isDeparturePlatformNormal,
                                                    Duration.standardSeconds(departureDelay),
                                                    isDepartureCanceled,
                                                    hasDeparted,
                                                    jsonLeg.getString("departureUri"),
                                                    TransportOccupancyLevel.UNSUPPORTED);

            boolean isArrivalCanceled = parseBooleanIfPresent(jsonLeg, false, "isArrivalCanceled");
            boolean isArrivalPlatformNormal = parseBooleanIfPresent(jsonLeg, true, "isArrivalPlatformNormal");
            boolean hasArrived = parseBooleanIfPresent(jsonLeg, false, "hasArrived");
            DateTime arrivalTime = DateTime.parse(jsonLeg.getString("arrivalTime"), dtf);
            int arrivalDelay = jsonLeg.getInt("arrivalDelay");

            RouteLegEnd arrival = new RouteLegEndImpl(arrivalStation,
                                                       arrivalTime,
                                                       jsonLeg.getString("arrivalPlatform"),
                                                       isArrivalPlatformNormal,
                                                       Duration.standardSeconds(arrivalDelay),
                                                       isArrivalCanceled,
                                                       hasArrived,
                                                       jsonLeg.getString("arrivalUri"),
                                                       TransportOccupancyLevel.UNSUPPORTED);

            legs[i] = new RouteLegImpl(RouteLegType.TRAIN, vehicle, departure, arrival);
        }
        return new RouteImpl(legs);
    }

    private boolean parseBooleanIfPresent(JSONObject object, boolean defaultValue, String isDepartureCanceled2) throws JSONException {
        if (object.has(isDepartureCanceled2)) {
            defaultValue = object.getBoolean(isDepartureCanceled2);
        }
        return defaultValue;
    }
}
