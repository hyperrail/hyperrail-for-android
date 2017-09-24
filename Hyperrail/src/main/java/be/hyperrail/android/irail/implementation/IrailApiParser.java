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

package be.hyperrail.android.irail.implementation;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import be.hyperrail.android.irail.contracts.IrailParser;
import be.hyperrail.android.irail.contracts.IrailStationProvider;
import be.hyperrail.android.irail.contracts.OccupancyLevel;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.db.Station;

/**
 * A simple parser for api.irail.be.
 *
 * @inheritDoc
 */
public class IrailApiParser implements IrailParser {

    private final IrailStationProvider stationProvider;

    public IrailApiParser(IrailStationProvider stationProvider) {
        this.stationProvider = stationProvider;
    }

    public RouteResult parseRouteResult(JSONObject json, Station origin, Station destination, DateTime searchTime, RouteTimeDefinition timeDefinition) throws JSONException {
        JSONArray routesObject = json.getJSONArray("connection");
        Route[] routes = new Route[routesObject.length()];
        for (int i = 0; i < routesObject.length(); i++) {
            routes[i] = parseRoute(routesObject.getJSONObject(i));
        }
        return new RouteResult(origin, destination, searchTime, timeDefinition, routes);
    }

    public Route parseRoute(JSONObject routeObject) throws JSONException {
        JSONObject departure = routeObject.getJSONObject("departure");
        JSONObject arrival = routeObject.getJSONObject("arrival");

        TrainStub firstTrain = new TrainStub(
                departure.getString("vehicle"),
                stationProvider.getStationByName(departure.getJSONObject("direction").getString("name")));

        TrainStub lastTrain = new TrainStub(
                arrival.getString("vehicle"),
                stationProvider.getStationByName(arrival.getJSONObject("direction").getString("name")));

        OccupancyLevel departureOccupancyLevel = OccupancyLevel.UNKNOWN;
        if (departure.has("occupancy")) {
            departureOccupancyLevel = OccupancyLevel.valueOf(departure.getJSONObject("occupancy").getString("name").toUpperCase());
        }
        Transfer departureTransfer = new Transfer(
                stationProvider.getStationById(departure.getJSONObject("stationinfo").getString("id")),
                null,
                firstTrain,
                null,
                true,
                departure.getString("platform"),
                departure.getJSONObject("platforminfo").getInt("normal") == 1,
                null,
                timestamp2date(departure.getString("time")),
                new Duration(0),
                false,
                new Duration(departure.getInt("delay") * 1000),
                departure.getInt("canceled") != 0,
                departure.getString("departureConnection"),
                departureOccupancyLevel
        );
        Station departureStation = departureTransfer.getStation();

        Transfer arrivalTransfer = new Transfer(
                stationProvider.getStationById(arrival.getJSONObject("stationinfo").getString("id")),
                lastTrain,
                null,
                arrival.getString("platform"),
                arrival.getJSONObject("platforminfo").getInt("normal") == 1,
                null,
                true,
                timestamp2date(arrival.getString("time")),
                null,
                new Duration(arrival.getInt("delay") * 1000),
                arrival.getInt("canceled") != 0,
                new Duration(0), false,
                null,
                OccupancyLevel.UNKNOWN
        );

        Station arrivalStation = arrivalTransfer.getStation();

        TrainStub[] trains;
        Transfer[] transfers;

        if (routeObject.has("vias")) {

            JSONObject vias = routeObject.getJSONObject("vias");
            int viaCount = vias.getInt("number");

            trains = new TrainStub[1 + viaCount];
            trains[0] = firstTrain;
            trains[trains.length - 1] = lastTrain;

            transfers = new Transfer[viaCount + 2];
            transfers[0] = departureTransfer;
            transfers[transfers.length - 1] = arrivalTransfer;

            for (int i = 0; i < viaCount; i++) {

                JSONObject via = (JSONObject) routeObject.getJSONObject("vias").getJSONArray("via").get(i);

                JSONObject viaDeparture = via.getJSONObject("departure");
                JSONObject viaArrival = via.getJSONObject("arrival");

                // first train is already set
                if (i > 0) {
                    trains[i] = new TrainStub(
                            via.getString("vehicle"),
                            stationProvider.getStationByName(via.getJSONObject("direction").getString("name")));
                }

                OccupancyLevel viaOccupancyLevel = OccupancyLevel.UNKNOWN;
                if (viaDeparture.has("occupancy")) {
                    viaOccupancyLevel = OccupancyLevel.valueOf(viaDeparture.getJSONObject("occupancy").getString("name").toUpperCase());
                }

                // don't use parseStop function, we have to combine data!
                Transfer s = new Transfer(
                        stationProvider.getStationById(via.getJSONObject("stationinfo").getString("id")),
                        trains[i],
                        trains[i + 1],
                        viaArrival.getString("platform"),
                        viaArrival.getJSONObject("platforminfo").getInt("normal") == 1,
                        viaDeparture.getString("platform"),
                        viaDeparture.getJSONObject("platforminfo").getInt("normal") == 1,
                        timestamp2date(viaArrival.getString("time")),
                        timestamp2date(viaDeparture.getString("time")),
                        new Duration(viaArrival.getInt("delay") * 1000),
                        viaArrival.getInt("canceled") != 0,
                        new Duration(viaDeparture.getInt("delay") * 1000),
                        viaDeparture.getInt("canceled") != 0,
                        viaDeparture.getString("departureConnection"),
                        viaOccupancyLevel
                );
                transfers[i + 1] = s;
            }
        } else {
            trains = new TrainStub[1];
            trains[0] = firstTrain;

            transfers = new Transfer[2];
            transfers[0] = departureTransfer;
            transfers[1] = arrivalTransfer;
        }

        return new Route(
                departureStation,
                arrivalStation,
                timestamp2date(departure.getString("time")),
                new Duration(departure.getInt("delay") * 1000),
                departure.getString("platform"),
                departure.getJSONObject("platforminfo").getInt("normal") == 1,
                timestamp2date(arrival.getString("time")),
                new Duration(arrival.getInt("delay") * 1000),
                arrival.getString("platform"),
                arrival.getJSONObject("platforminfo").getInt("normal") == 1,
                trains,
                transfers);
    }

    @Override
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

    public LiveBoard parseLiveboard(JSONObject jsonData, DateTime searchDate) throws JSONException {

        if (jsonData == null) {
            throw new IllegalArgumentException("JSONObject is null");
        }

        JSONObject object;
        JSONArray items;

        Station s = stationProvider.getStationById(jsonData.getJSONObject("stationinfo").getString("id"));

        if (jsonData.has("departures")) {
            object = jsonData.getJSONObject("departures");
            items = object.getJSONArray("departure");
        } else if (jsonData.has("arrivals")) {
            object = jsonData.getJSONObject("arrivals");
            items = object.getJSONArray("arrival");
        } else {
            return new LiveBoard(s, new TrainStop[0], searchDate);
        }

        TrainStop[] stops = new TrainStop[items.length()];
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            stops[i] = parseLiveboardStop(s, item);
        }

        return new LiveBoard(
                s,
                stops,
                searchDate);
    }

    private TrainStop parseLiveboardStop(JSONObject item) throws JSONException {
        return parseLiveboardStop(stationProvider.getStationByName(item.getString("station")), item);
    }

    // allow providing station, so liveboards don't need to parse a station over and over
    private TrainStop parseLiveboardStop(Station stop, JSONObject item) throws JSONException {
        Station destination = stationProvider.getStationById(item.getJSONObject("stationinfo").getString("id"));

        OccupancyLevel occupancyLevel = OccupancyLevel.UNKNOWN;
        if (item.has("occupancy")) {
            occupancyLevel = OccupancyLevel.valueOf(item.getJSONObject("occupancy").getString("name").toUpperCase());
        }

        return new TrainStop(
                stop,
                destination,
                new TrainStub(item.getString("vehicle"), destination),
                item.getString("platform"),
                item.getJSONObject("platforminfo").getInt("normal") == 1,
                timestamp2date(item.getString("time")),
                new Duration(item.getInt("delay") * 1000),
                item.getInt("canceled") != 0,
                (item.has("left")) && (item.getInt("left") == 1),
                item.getString("departureConnection"),
                occupancyLevel
        );
    }

    // allow providing station, so liveboards don't need to parse a station over and over
    private TrainStop parseTrainStop(Station destination, TrainStub t, JSONObject item) throws JSONException {
        Station stop = stationProvider.getStationById(item.getJSONObject("stationinfo").getString("id"));

        OccupancyLevel occupancyLevel = OccupancyLevel.UNKNOWN;
        if (item.has("occupancy")) {
            occupancyLevel = OccupancyLevel.valueOf(item.getJSONObject("occupancy").getString("name").toUpperCase());
        }

        return new TrainStop(
                stop,
                destination,
                t,
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
                occupancyLevel
        );
    }

    public Train parseTrain(JSONObject jsonData, DateTime searchdate) throws JSONException {

        String id = jsonData.getString("vehicle");
        double longitude = jsonData.getJSONObject("vehicleinfo").getDouble("locationX");
        double latitude = jsonData.getJSONObject("vehicleinfo").getDouble("locationY");

        JSONArray jsonStops = jsonData.getJSONObject("stops").getJSONArray("stop");
        Station destination = stationProvider.getStationById(
                jsonStops
                        .getJSONObject(jsonStops.length() - 1)
                        .getJSONObject("stationinfo")
                        .getString("id")
        );

        TrainStop[] stops = new TrainStop[jsonStops.length()];
        TrainStub t = new TrainStub(id, destination);

        for (int i = 0; i < jsonStops.length(); i++) {
            stops[i] = parseTrainStop(destination, t, jsonStops.getJSONObject(i));
        }

        return new Train(id, destination, stops[0].getStation(), longitude, latitude, stops);
    }

    private static DateTime timestamp2date(String time) {
        return timestamp2date(Long.parseLong(time));
    }

    private static DateTime timestamp2date(long time) {
        return new DateTime(time * 1000);
    }
}
