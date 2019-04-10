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

package be.hyperrail.opentransportdata.be.irail;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition;
import be.hyperrail.opentransportdata.common.contracts.TransportOccupancyLevel;
import be.hyperrail.opentransportdata.common.contracts.TransportStopsDataSource;
import be.hyperrail.opentransportdata.common.exceptions.StopLocationNotResolvedException;
import be.hyperrail.opentransportdata.common.models.Disturbance;
import be.hyperrail.opentransportdata.common.models.LiveboardType;
import be.hyperrail.opentransportdata.common.models.Message;
import be.hyperrail.opentransportdata.common.models.Route;
import be.hyperrail.opentransportdata.common.models.RouteLeg;
import be.hyperrail.opentransportdata.common.models.RouteLegEnd;
import be.hyperrail.opentransportdata.common.models.RouteLegType;
import be.hyperrail.opentransportdata.common.models.StopLocation;
import be.hyperrail.opentransportdata.common.models.VehicleStopType;
import be.hyperrail.opentransportdata.common.models.implementation.DisturbanceImpl;
import be.hyperrail.opentransportdata.common.models.implementation.LiveboardImpl;
import be.hyperrail.opentransportdata.common.models.implementation.MessageImpl;
import be.hyperrail.opentransportdata.common.models.implementation.RouteImpl;
import be.hyperrail.opentransportdata.common.models.implementation.RouteLegEndImpl;
import be.hyperrail.opentransportdata.common.models.implementation.RouteLegImpl;
import be.hyperrail.opentransportdata.common.models.implementation.RoutesListImpl;
import be.hyperrail.opentransportdata.common.models.implementation.VehicleStopImpl;
import be.hyperrail.opentransportdata.logging.OpenTransportLog;

/**
 * A simple parser for api.irail.be.
 *
 * @inheritDoc
 */
class IrailApiParser {

    private final TransportStopsDataSource stationProvider;
    private static final OpenTransportLog log = OpenTransportLog.getLogger(IrailApiParser.class);

    IrailApiParser(TransportStopsDataSource stationProvider) {
        this.stationProvider = stationProvider;
    }

    RoutesListImpl parseRouteResult(JSONObject json, StopLocation origin, StopLocation destination, DateTime searchTime, QueryTimeDefinition timeDefinition) throws JSONException {
        JSONArray routesObject = json.getJSONArray("connection");
        List<Route> routeList = new ArrayList<>();
        for (int i = 0; i < routesObject.length(); i++) {
            try {
                routeList.add(parseRoute(routesObject.getJSONObject(i)));
            } catch (StopLocationNotResolvedException e) {
                log.warning("Failed to resolve station", e);
            }
        }
        Route[] routes = new Route[routeList.size()];
        routes = routeList.toArray(routes);
        return new RoutesListImpl(origin, destination, searchTime, timeDefinition, routes);
    }

    private Route parseRoute(JSONObject routeObject) throws JSONException, StopLocationNotResolvedException {
        JSONObject departure = routeObject.getJSONObject("departure");
        JSONObject arrival = routeObject.getJSONObject("arrival");

        IrailVehicleJourneyStub firstTrain = new IrailVehicleJourneyStub(
                departure.getString("vehicle"),
                departure.getJSONObject("direction").getString("name"),
                null);

        TransportOccupancyLevel departureOccupancyLevel = parseOccupancyLevel(departure);

        boolean hasLastTrainArrived = isNumericBooleanTrue(arrival, "arrived");
        boolean hasFirstTrainLeft = (isNumericBooleanTrue(departure, "left"));

        RouteLegEnd departureEnd = new RouteLegEndImpl(
                stationProvider.getStoplocationBySemanticId(getStationUri(departure)),
                timestamp2date(departure.getString("time")), departure.getString("platform"), isPlatformNormal(departure),
                delayToDuration(departure, "delay"), isCanceled(departure), hasFirstTrainLeft,
                departure.getString("departureConnection"),
                departureOccupancyLevel);

        RouteLegEnd arrivalEnd = new RouteLegEndImpl(
                stationProvider.getStoplocationBySemanticId(getStationUri(arrival)),
                timestamp2date(arrival.getString("time")), arrival.getString("platform"), isPlatformNormal(arrival),
                delayToDuration(arrival, "delay"), isCanceled(arrival), hasLastTrainArrived, null, null);


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

                TransportOccupancyLevel viaOccupancyLevel = parseOccupancyLevel(viaDeparture);

                boolean hasArrived = (isNumericBooleanTrue(viaArrival, "arrived"));
                boolean hasLeft = (isNumericBooleanTrue(viaDeparture, "left"));

                // don't use parseStop function, we have to combine data!
                arrivals[i] = new RouteLegEndImpl(
                        stationProvider.getStoplocationBySemanticId(getStationUri(via)),
                        timestamp2date(viaArrival.getString("time")), viaArrival.getString("platform"), isPlatformNormal(viaArrival),
                        delayToDuration(viaArrival, "delay"), isCanceled(viaArrival), hasArrived, viaArrival.getString("departureConnection"), null);
                departures[i + 1] = new RouteLegEndImpl(
                        stationProvider.getStoplocationBySemanticId(getStationUri(via)),
                        timestamp2date(viaDeparture.getString("time")), viaDeparture.getString("platform"), isPlatformNormal(viaDeparture),
                        delayToDuration(viaDeparture, "delay"), isCanceled(viaDeparture), hasLeft,
                        viaDeparture.getString("departureConnection"), viaOccupancyLevel);
            }

            legs = new RouteLeg[viaCount + 1];

            if (departure.getInt("walking") == 0) {
                legs[0] = new RouteLegImpl(RouteLegType.TRAIN,
                        new IrailVehicleJourneyStub(
                                irailIdToVehicleId(departure),
                                departure.getJSONObject("direction").getString("name"), null),
                        departures[0], arrivals[0]);
            } else {
                legs[0] = new RouteLegImpl(RouteLegType.WALK, null, departures[0], arrivals[0]);
            }

            for (int i = 0; i < viaCount; i++) {
                JSONObject via = (JSONObject) routeObject.getJSONObject("vias").getJSONArray("via").get(i);
                JSONObject viaDeparture = via.getJSONObject("departure");
                // first train is already set
                // Walking should only be between 2 journeys, so only in a via
                if (viaDeparture.getInt("walking") == 0) {
                    legs[i + 1] = new RouteLegImpl(RouteLegType.TRAIN,
                            new IrailVehicleJourneyStub(
                                    irailIdToVehicleId(viaDeparture),
                                    viaDeparture.getJSONObject("direction").getString("name"), null),
                            departures[i + 1], arrivals[i + 1]);
                } else {
                    legs[i + 1] = new RouteLegImpl(RouteLegType.WALK, null, departures[i + 1], arrivals[i + 1]);
                }
            }

        } else {
            legs = new RouteLeg[1];
            legs[0] = new RouteLegImpl(RouteLegType.TRAIN, firstTrain, departureEnd, arrivalEnd);
        }

        Message[][] trainalerts = parseRouteAlertsPerLeg(routeObject, departure, legs);

        Message[] alerts = parseRouteAlerts(routeObject);

        Route r = new RouteImpl(legs);
        r.setAlerts(alerts);
        r.setVehicleAlerts(trainalerts);
        return r;
    }

    private boolean isNumericBooleanTrue(JSONObject arrival, String arrived) throws JSONException {
        return arrival.has(arrived) && arrival.getInt(arrived) == 1;
    }

    @NonNull
    private String irailIdToVehicleId(JSONObject viaDeparture) throws JSONException {
        return viaDeparture.getString("vehicle").substring(8);
    }

    private boolean isCanceled(JSONObject viaDeparture) throws JSONException {
        return viaDeparture.getInt("canceled") != 0;
    }

    private boolean isPlatformNormal(JSONObject jsonObject) throws JSONException {
        return jsonObject.getJSONObject("platforminfo").getInt("normal") == 1;
    }

    private String getStationUri(JSONObject object) throws JSONException {
        return object.getJSONObject("stationinfo").getString("@id");
    }

    @NonNull
    private Message[][] parseRouteAlertsPerLeg(JSONObject routeObject, JSONObject departure, RouteLeg[] legs) throws JSONException {
        Message[][] alertsPerLeg = new Message[legs.length][];
        for (int legIndex = 0; legIndex < alertsPerLeg.length; legIndex++) {
            if (legIndex == 0) {
                parseAlertsForRouteLeg(alertsPerLeg, legIndex, departure);
            } else {
                JSONObject viaDeparture = routeObject.getJSONObject("vias").getJSONArray("via").getJSONObject(legIndex - 1).getJSONObject("departure");
                parseAlertsForRouteLeg(alertsPerLeg, legIndex, viaDeparture);
            }
        }
        return alertsPerLeg;
    }

    private void parseAlertsForRouteLeg(Message[][] alertsPerLeg, int legIndex, JSONObject viaDeparture) throws JSONException {
        if (viaDeparture.has("alerts")) {
            JSONArray alerts = viaDeparture.getJSONObject("alerts").getJSONArray("alert");
            alertsPerLeg[legIndex] = new Message[alerts.length()];
            for (int i = 0; i < alerts.length(); i++) {
                alertsPerLeg[legIndex][i] = parseMessage(alerts.getJSONObject(i));
            }
        } else {
            alertsPerLeg[legIndex] = null;
        }
    }

    @Nullable
    private Message[] parseRouteAlerts(JSONObject routeObject) throws JSONException {
        Message[] alerts = null;
        if (routeObject.has("alerts")) {
            JSONArray alertsArray = routeObject.getJSONObject("alerts").getJSONArray("alert");
            alerts = new Message[alertsArray.length()];
            for (int i = 0; i < alertsArray.length(); i++) {
                alerts[i] = parseMessage(alertsArray.getJSONObject(i));
            }
        }
        return alerts;
    }

    private Message parseMessage(JSONObject json) {
        try {
            String header = json.getString("header");
            String description = json.getString("description");
            String link = "";
            if (json.has("link")) {
                link = json.getString("link");
            }
            return new MessageImpl(header, description, link);
        } catch (JSONException e) {
            log.severe("Failed to parse json message");
        }
        return null;
    }

    Disturbance[] parseDisturbances(JSONObject jsonData) throws JSONException {

        if (jsonData == null) {
            throw new IllegalArgumentException("JSONObject is null");
        }

        if (!jsonData.has("disturbance")) {
            return new Disturbance[0];
        }

        JSONArray items = jsonData.getJSONArray("disturbance");

        Disturbance[] result = new Disturbance[items.length()];

        for (int i = 0; i < items.length(); i++) {
            result[i] = new DisturbanceImpl(i,
                    timestamp2date(items.getJSONObject(i).getString("timestamp")),
                    items.getJSONObject(i).getString("title"),
                    items.getJSONObject(i).getString("description"),
                    items.getJSONObject(i).getString("link"));
        }

        return result;
    }

    LiveboardImpl parseLiveboard(JSONObject jsonData, DateTime searchDate, LiveboardType type, QueryTimeDefinition timeDefinition) throws JSONException, StopLocationNotResolvedException {

        if (jsonData == null) {
            throw new IllegalArgumentException("JSONObject is null");
        }

        JSONObject object;
        JSONArray items;

        StopLocation stopLocation = stationProvider.getStoplocationBySemanticId(getStationUri(jsonData));

        if (jsonData.has("departures")) {
            object = jsonData.getJSONObject("departures");
            items = object.getJSONArray("departure");
        } else if (jsonData.has("arrivals")) {
            object = jsonData.getJSONObject("arrivals");
            items = object.getJSONArray("arrival");
        } else {
            return new LiveboardImpl(stopLocation, new VehicleStopImpl[0], searchDate, type, timeDefinition);
        }

        VehicleStopImpl[] vehicleStops = new VehicleStopImpl[items.length()];
        for (int i = 0; i < items.length(); i++) {
            JSONObject vehicleStopJson = items.getJSONObject(i);
            if (type == LiveboardType.DEPARTURES) {
                vehicleStops[i] = parseLiveboardStop(stopLocation, vehicleStopJson, VehicleStopType.DEPARTURE);
            } else {
                vehicleStops[i] = parseLiveboardStop(stopLocation, vehicleStopJson, VehicleStopType.ARRIVAL);
            }
        }

        return new LiveboardImpl(
                stopLocation,
                vehicleStops,
                searchDate,
                type,
                timeDefinition);
    }

    // allow providing station, so liveboards don't need to parse a station over and over

    private VehicleStopImpl parseLiveboardStop(StopLocation stop, JSONObject vehicleStopJson, VehicleStopType type) throws JSONException {

        String headsign = parseHeadsign(vehicleStopJson);

        TransportOccupancyLevel occupancyLevel = parseOccupancyLevel(vehicleStopJson);
        if (type == VehicleStopType.DEPARTURE) {
            return buildLiveboardDepartureStop(stop, vehicleStopJson, headsign, occupancyLevel);
        } else {
            return buildLiveboardArrivalStop(stop, vehicleStopJson, headsign, occupancyLevel);
        }
    }

    @NonNull
    private TransportOccupancyLevel parseOccupancyLevel(JSONObject vehicleStopJson) throws JSONException {
        TransportOccupancyLevel occupancyLevel = TransportOccupancyLevel.UNKNOWN;
        if (vehicleStopJson.has("occupancy")) {
            occupancyLevel = TransportOccupancyLevel.valueOf(vehicleStopJson.getJSONObject("occupancy").getString("name").toUpperCase());
        }
        return occupancyLevel;
    }


    private String parseHeadsign(JSONObject item) throws JSONException {
        String headsign;

        try {
            // Try loading a localized name from the stations database
            StopLocation destination = stationProvider.getStoplocationBySemanticId(getStationUri(item));
            headsign = destination.getLocalizedName();
        } catch (Exception e) {
            // If this fails, just use the provided name
            headsign = item.getJSONObject("stationinfo").getString("name");
        }
        return headsign;
    }

    @NonNull
    private VehicleStopImpl buildLiveboardArrivalStop(StopLocation stop, JSONObject item, String headsign, TransportOccupancyLevel occupancyLevel) throws JSONException {
        return VehicleStopImpl.buildArrivalVehicleStop(
                stop,
                new IrailVehicleJourneyStub(irailIdToVehicleId(item), headsign, item.getJSONObject("vehicleinfo").getString("@id")),
                item.getString("platform"),
                isPlatformNormal(item),
                timestamp2date(item.getString("time")),
                delayToDuration(item, "delay"),
                isCanceled(item),
                isNumericBooleanTrue(item, "left"),
                item.getString("departureConnection"),
                occupancyLevel);
    }

    @NonNull
    private VehicleStopImpl buildLiveboardDepartureStop(StopLocation stop, JSONObject item, String headsign, TransportOccupancyLevel occupancyLevel) throws JSONException {
        return VehicleStopImpl.buildDepartureVehicleStop(
                stop,
                new IrailVehicleJourneyStub(irailIdToVehicleId(item), headsign, item.getJSONObject("vehicleinfo").getString("@id")),
                item.getString("platform"),
                isPlatformNormal(item),
                timestamp2date(item.getString("time")),
                delayToDuration(item, "delay"),
                isCanceled(item),
                isNumericBooleanTrue(item, "left"),
                item.getString("departureConnection"),
                occupancyLevel
        );
    }

    @NonNull
    private Duration delayToDuration(JSONObject item, String delay) throws JSONException {
        return new Duration(item.getInt(delay) * 1000);
    }

    // allow providing station, so don't need to parse a station over and over

    private VehicleStopImpl parseTrainStop(IrailVehicleJourneyStub train, JSONObject item, VehicleStopType type) throws JSONException, StopLocationNotResolvedException {
        StopLocation stop = stationProvider.getStoplocationBySemanticId(getStationUri(item));

        TransportOccupancyLevel occupancyLevel = parseOccupancyLevel(item);

        return new VehicleStopImpl(
                stop,
                train,
                item.getString("platform"),
                isPlatformNormal(item),
                timestamp2date(item.getString("scheduledDepartureTime")),
                timestamp2date(item.getString("scheduledArrivalTime")),
                delayToDuration(item, "departureDelay"),
                delayToDuration(item, "arrivalDelay"),
                isNumericBooleanTrue(item, "departureCanceled"),
                isNumericBooleanTrue(item, "arrivalCanceled"),
                isNumericBooleanTrue(item, "left"),
                item.getString("departureConnection"),
                occupancyLevel,
                type);
    }

    IrailVehicleJourney parseVehicleJourney(JSONObject jsonData) throws JSONException, StopLocationNotResolvedException {

        String id = irailIdToVehicleId(jsonData);
        String uri = jsonData.getJSONObject("vehicleinfo").getString("@id");
        double longitude = jsonData.getJSONObject("vehicleinfo").getDouble("locationX");
        double latitude = jsonData.getJSONObject("vehicleinfo").getDouble("locationY");

        JSONArray jsonStops = jsonData.getJSONObject("stops").getJSONArray("stop");

        // Try to load the localized name of the last station
        String headsign = parseHeadsign(jsonStops.getJSONObject(jsonStops.length() - 1));
        VehicleStopImpl[] stops = new VehicleStopImpl[jsonStops.length()];
        IrailVehicleJourneyStub t = new IrailVehicleJourneyStub(id,
                headsign,
                uri);

        for (int i = 0; i < jsonStops.length(); i++) {
            VehicleStopType type = VehicleStopType.STOP;
            if (i == 0) {
                type = VehicleStopType.DEPARTURE;
            } else if (i == jsonStops.length() - 1) {
                type = VehicleStopType.ARRIVAL;
            }
            stops[i] = parseTrainStop(t, jsonStops.getJSONObject(i), type);
        }

        // Consider whether or not the searchDate should be stored
        return new IrailVehicleJourney(id, uri, longitude, latitude, stops);
    }


    private static DateTime timestamp2date(String time) {
        return timestamp2date(Long.parseLong(time)).withZone(DateTimeZone.forID("Europe/Brussels"));
    }


    private static DateTime timestamp2date(long time) {
        return new DateTime(time * 1000).withZone(DateTimeZone.forID("Europe/Brussels"));
    }
}
