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

package eu.opentransport.common.models;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import eu.opentransport.common.contracts.QueryTimeDefinition;
import eu.opentransport.common.contracts.TransportOccupancyLevel;
import eu.opentransport.common.contracts.TransportStopsDataSource;
import eu.opentransport.common.exceptions.StopLocationNotResolvedException;

/**
 * A simple parser for api.irail.be.
 *
 * @inheritDoc
 */
public class IrailApiParser {

    private final TransportStopsDataSource stationProvider;

    public IrailApiParser(TransportStopsDataSource stationProvider) {
        this.stationProvider = stationProvider;
    }

    public RouteResult parseRouteResult(JSONObject json, Station origin, Station destination, DateTime searchTime, QueryTimeDefinition timeDefinition) throws JSONException {
        JSONArray routesObject = json.getJSONArray("connection");
        List<Route> routeList = new ArrayList<>();
        for (int i = 0; i < routesObject.length(); i++) {
            try {
                routeList.add(parseRoute(routesObject.getJSONObject(i)));
            } catch (StopLocationNotResolvedException e) {
                e.printStackTrace();
            }
        }
        Route[] routes = new Route[routeList.size()];
        routes = routeList.toArray(routes);
        return new RouteResult(origin, destination, searchTime, timeDefinition, routes);
    }

    private Route parseRoute(JSONObject routeObject) throws JSONException, StopLocationNotResolvedException {
        JSONObject departure = routeObject.getJSONObject("departure");
        JSONObject arrival = routeObject.getJSONObject("arrival");

        VehicleStub firstTrain = new VehicleStub(
                departure.getString("vehicle"),
                departure.getJSONObject("direction").getString("name"),
                null);

        TransportOccupancyLevel departureOccupancyLevel = TransportOccupancyLevel.UNKNOWN;
        if (departure.has("occupancy")) {
            departureOccupancyLevel = TransportOccupancyLevel.valueOf(departure.getJSONObject("occupancy").getString("name").toUpperCase());
        }

        boolean hasLastTrainArrived = (arrival.has("arrived") && arrival.getInt("arrived") == 1);
        boolean hasFirstTrainLeft = (departure.has("left") && departure.getInt("left") == 1);

        RouteLegEnd departureEnd = new RouteLegEnd(
                stationProvider.getStationByIrailApiId(departure.getJSONObject("stationinfo").getString("id")),
                timestamp2date(departure.getString("time")), departure.getString("platform"), departure.getJSONObject("platforminfo").getInt("normal") == 1,
                new Duration(departure.getInt("delay") * 1000), departure.getInt("canceled") != 0, hasFirstTrainLeft,
                departure.getString("departureConnection"),
                departureOccupancyLevel);

        RouteLegEnd arrivalEnd = new RouteLegEnd(
                stationProvider.getStationByIrailApiId(arrival.getJSONObject("stationinfo").getString("id")),
                timestamp2date(arrival.getString("time")), arrival.getString("platform"), arrival.getJSONObject("platforminfo").getInt("normal") == 1,
                new Duration(arrival.getInt("delay") * 1000), arrival.getInt("canceled") != 0, hasLastTrainArrived, null, null);


        RouteLeg[] legs;

        if (routeObject.has("vias")) {

            JSONObject vias = routeObject.getJSONObject("vias");
            int viaCount = vias.getInt("number");


            RouteLegEnd[] departures = new RouteLegEnd[viaCount + 1];
            RouteLegEnd[] arrivals = new RouteLegEnd[viaCount + 1];
            departures[0] = departureEnd;
            arrivals[arrivals.length - 1] = arrivalEnd;

            for (int i = 0; i < viaCount; i++) {

                JSONObject via = (JSONObject) routeObject.getJSONObject("vias").getJSONArray("via").get(i);

                JSONObject viaDeparture = via.getJSONObject("departure");
                JSONObject viaArrival = via.getJSONObject("arrival");

                TransportOccupancyLevel viaOccupancyLevel = TransportOccupancyLevel.UNKNOWN;
                if (viaDeparture.has("occupancy")) {
                    viaOccupancyLevel = TransportOccupancyLevel.valueOf(viaDeparture.getJSONObject("occupancy").getString("name").toUpperCase());
                }

                boolean hasArrived = (viaArrival.has("arrived") && viaArrival.getInt("arrived") == 1);
                boolean hasLeft = (viaDeparture.has("left") && viaDeparture.getInt("left") == 1);

                // don't use parseStop function, we have to combine data!
                arrivals[i] = new RouteLegEnd(
                        stationProvider.getStationByIrailApiId(via.getJSONObject("stationinfo").getString("id")),
                        timestamp2date(viaArrival.getString("time")), viaArrival.getString("platform"), viaArrival.getJSONObject("platforminfo").getInt("normal") == 1,
                        new Duration(viaArrival.getInt("delay") * 1000), viaArrival.getInt("canceled") != 0, hasArrived, null, null);
                departures[i + 1] = new RouteLegEnd(
                        stationProvider.getStationByIrailApiId(via.getJSONObject("stationinfo").getString("id")),
                        timestamp2date(viaDeparture.getString("time")), viaDeparture.getString("platform"), viaDeparture.getJSONObject("platforminfo").getInt("normal") == 1,
                        new Duration(viaDeparture.getInt("delay") * 1000), viaDeparture.getInt("canceled") != 0, hasLeft,
                        viaDeparture.getString("departureConnection"), viaOccupancyLevel);
            }

            legs = new RouteLeg[viaCount + 1];

            if (departure.getInt("walking") == 0) {
                legs[0] = new RouteLeg(RouteLegType.TRAIN,
                                       new VehicleStub(
                                               departure.getString("vehicle").substring(8),
                                               departure.getJSONObject("direction").getString("name"), null),
                                       departures[0], arrivals[0]);
            } else {
                legs[0] = new RouteLeg(RouteLegType.WALK, null, departures[0], arrivals[0]);
            }

            for (int i = 0; i < viaCount; i++) {
                JSONObject via = (JSONObject) routeObject.getJSONObject("vias").getJSONArray("via").get(i);
                JSONObject viaDeparture = via.getJSONObject("departure");
                // first train is already set
                // Walking should only be between 2 journeys, so only in a via
                if (viaDeparture.getInt("walking") == 0) {
                    legs[i + 1] = new RouteLeg(RouteLegType.TRAIN,
                                               new VehicleStub(
                                                       viaDeparture.getString("vehicle").substring(8),
                                                       viaDeparture.getJSONObject("direction").getString("name"), null),
                                               departures[i + 1], arrivals[i + 1]);
                } else {
                    legs[i + 1] = new RouteLeg(RouteLegType.WALK, null, departures[i + 1], arrivals[i + 1]);
                }
            }

        } else {
            legs = new RouteLeg[1];
            legs[0] = new RouteLeg(RouteLegType.TRAIN, firstTrain, departureEnd, arrivalEnd);
        }

        Message[][] trainalerts = new Message[legs.length][];
        for (int t = 0; t < trainalerts.length; t++) {
            if (t == 0) {
                if (departure.has("alerts")) {
                    JSONArray alerts = departure.getJSONObject("alerts").getJSONArray("alert");
                    trainalerts[t] = new Message[alerts.length()];
                    for (int i = 0; i < alerts.length(); i++) {
                        trainalerts[t][i] = new Message(alerts.getJSONObject(i));
                    }
                } else {
                    trainalerts[t] = null;
                }
            } else {
                JSONObject viaDeparture = routeObject.getJSONObject("vias").getJSONArray("via").getJSONObject(t - 1).getJSONObject("departure");

                if (viaDeparture.has("alerts")) {
                    JSONArray alerts = viaDeparture.getJSONObject("alerts").getJSONArray("alert");
                    trainalerts[t] = new Message[alerts.length()];
                    for (int i = 0; i < alerts.length(); i++) {
                        trainalerts[t][i] = new Message(alerts.getJSONObject(i));
                    }
                } else {
                    trainalerts[t] = null;
                }
            }
        }

        Message[] alerts = null;
        if (routeObject.has("alerts")) {
            JSONArray alertsArray = routeObject.getJSONObject("alerts").getJSONArray("alert");
            alerts = new Message[alertsArray.length()];
            for (int i = 0; i < alertsArray.length(); i++) {
                alerts[i] = new Message(alertsArray.getJSONObject(i));
            }
        }

        Route r = new Route(legs);
        r.setAlerts(alerts);
        r.setTrainalerts(trainalerts);
        return r;
    }

    public Disturbance[] parseDisturbances(JSONObject jsonData) throws JSONException {

        if (jsonData == null) {
            throw new IllegalArgumentException("JSONObject is null");
        }

        if (!jsonData.has("disturbance")) {
            return new Disturbance[0];
        }

        JSONArray items = jsonData.getJSONArray("disturbance");

        Disturbance[] result = new Disturbance[items.length()];

        for (int i = 0; i < items.length(); i++) {
            result[i] = new Disturbance(i,
                                        timestamp2date(items.getJSONObject(i).getString("timestamp")),
                                        items.getJSONObject(i).getString("title"),
                                        items.getJSONObject(i).getString("description"),
                                        items.getJSONObject(i).getString("link"));
        }

        return result;
    }

    public Liveboard parseLiveboard(JSONObject jsonData, DateTime searchDate, Liveboard.LiveboardType type, QueryTimeDefinition timeDefinition) throws JSONException, StopLocationNotResolvedException {

        if (jsonData == null) {
            throw new IllegalArgumentException("JSONObject is null");
        }

        JSONObject object;
        JSONArray items;

        Station s = stationProvider.getStationByIrailApiId(jsonData.getJSONObject("stationinfo").getString("id"));

        if (jsonData.has("departures")) {
            object = jsonData.getJSONObject("departures");
            items = object.getJSONArray("departure");
        } else if (jsonData.has("arrivals")) {
            object = jsonData.getJSONObject("arrivals");
            items = object.getJSONArray("arrival");
        } else {
            return new Liveboard(s, new VehicleStop[0], searchDate, type, timeDefinition);
        }

        VehicleStop[] stops = new VehicleStop[items.length()];
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            if (type == Liveboard.LiveboardType.DEPARTURES) {
                stops[i] = parseLiveboardStop(s, item, VehicleStopType.DEPARTURE);
            } else {
                stops[i] = parseLiveboardStop(s, item, VehicleStopType.ARRIVAL);
            }
        }

        return new Liveboard(
                s,
                stops,
                searchDate,
                type,
                timeDefinition);
    }

    // allow providing station, so liveboards don't need to parse a station over and over

    private VehicleStop parseLiveboardStop(Station stop, JSONObject item, VehicleStopType type) throws JSONException {

        String headsign;

        try {
            Station destination = stationProvider.getStationByIrailApiId(item.getJSONObject("stationinfo").getString("id"));
            headsign = destination.getLocalizedName();
        } catch (Exception e) {
            headsign = item.getJSONObject("stationinfo").getString("name");
        }

        TransportOccupancyLevel occupancyLevel = TransportOccupancyLevel.UNKNOWN;
        if (item.has("occupancy")) {
            occupancyLevel = TransportOccupancyLevel.valueOf(item.getJSONObject("occupancy").getString("name").toUpperCase());
        }
        if (type == VehicleStopType.DEPARTURE) {
            return VehicleStop.buildDepartureVehicleStop(
                    stop,
                    new VehicleStub(item.getString("vehicle").substring(8), headsign, item.getJSONObject("vehicleinfo").getString("@id")),
                    item.getString("platform"),
                    item.getJSONObject("platforminfo").getInt("normal") == 1,
                    timestamp2date(item.getString("time")),
                    new Duration(item.getInt("delay") * 1000),
                    item.getInt("canceled") != 0,
                    (item.has("left")) && (item.getInt("left") == 1),
                    item.getString("departureConnection"),
                    occupancyLevel
            );
        } else {
            return VehicleStop.buildArrivalVehicleStop(
                    stop,
                    new VehicleStub(item.getString("vehicle").substring(8), headsign, item.getJSONObject("vehicleinfo").getString("@id")),
                    item.getString("platform"),
                    item.getJSONObject("platforminfo").getInt("normal") == 1,
                    timestamp2date(item.getString("time")),
                    new Duration(item.getInt("delay") * 1000),
                    item.getInt("canceled") != 0,
                    (item.has("left")) && (item.getInt("left") == 1),
                    item.getString("departureConnection"),
                    occupancyLevel);
        }
    }

    // allow providing station, so don't need to parse a station over and over

    private VehicleStop parseTrainStop(String headsign, VehicleStub train, JSONObject item, VehicleStopType type) throws JSONException, StopLocationNotResolvedException {
        Station stop = stationProvider.getStationByIrailApiId(item.getJSONObject("stationinfo").getString("id"));

        TransportOccupancyLevel occupancyLevel = TransportOccupancyLevel.UNKNOWN;
        if (item.has("occupancy")) {
            occupancyLevel = TransportOccupancyLevel.valueOf(item.getJSONObject("occupancy").getString("name").toUpperCase());
        }

        return new VehicleStop(
                stop,
                train,
                item.getString("platform"),
                item.getJSONObject("platforminfo").getInt("normal") == 1,
                timestamp2date(item.getString("scheduledDepartureTime")),
                timestamp2date(item.getString("scheduledArrivalTime")),
                new Duration(item.getInt("departureDelay") * 1000),
                new Duration(item.getInt("arrivalDelay") * 1000),
                item.getInt("departureCanceled") != 0,
                item.getInt("arrivalCanceled") != 0,
                item.getInt("left") == 1,
                item.getString("departureConnection"),
                occupancyLevel,
                type);
    }

    public Vehicle parseTrain(JSONObject jsonData, DateTime searchdate) throws JSONException, StopLocationNotResolvedException {

        String id = jsonData.getString("vehicle").substring(8);
        String uri = jsonData.getJSONObject("vehicleinfo").getString("@id");
        double longitude = jsonData.getJSONObject("vehicleinfo").getDouble("locationX");
        double latitude = jsonData.getJSONObject("vehicleinfo").getDouble("locationY");

        JSONArray jsonStops = jsonData.getJSONObject("stops").getJSONArray("stop");

        String headsign;
        try {
            Station destination = stationProvider.getStationByIrailApiId(
                    jsonStops
                            .getJSONObject(jsonStops.length() - 1)
                            .getJSONObject("stationinfo")
                            .getString("id")
            );
            headsign = destination.getLocalizedName();
        } catch (Exception e) {
            headsign = jsonStops
                    .getJSONObject(jsonStops.length() - 1)
                    .getJSONObject("stationinfo")
                    .getString("name");
        }
        VehicleStop[] stops = new VehicleStop[jsonStops.length()];
        VehicleStub t = new VehicleStub(id,
                                        jsonStops.getJSONObject(jsonStops.length() - 1)
                                                .getJSONObject("stationinfo")
                                                .getString("name"),
                                        uri);

        for (int i = 0; i < jsonStops.length(); i++) {
            VehicleStopType type = VehicleStopType.STOP;
            if (i == 0) {
                type = VehicleStopType.DEPARTURE;
            } else if (i == jsonStops.length() - 1) {
                type = VehicleStopType.ARRIVAL;
            }
            stops[i] = parseTrainStop(headsign, t, jsonStops.getJSONObject(i), type);
        }

        // Consider whether or not the searchDate should be stored
        return new Vehicle(id, uri, longitude, latitude, stops);
    }


    private static DateTime timestamp2date(String time) {
        return timestamp2date(Long.parseLong(time)).withZone(DateTimeZone.forID("Europe/Brussels"));
    }


    private static DateTime timestamp2date(long time) {
        return new DateTime(time * 1000).withZone(DateTimeZone.forID("Europe/Brussels"));
    }
}
