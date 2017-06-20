/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package android.irail.be.hyperrail.irail.implementation;

import android.irail.be.hyperrail.irail.contracts.IrailParser;
import android.irail.be.hyperrail.irail.contracts.IrailStationProvider;
import android.irail.be.hyperrail.irail.contracts.RouteTimeDefinition;
import android.irail.be.hyperrail.irail.db.Station;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * A simple parser for api.irail.be.
 * @inheritDoc
 */
public class IrailApiParser implements IrailParser {

    IrailStationProvider stationProvider;

    public IrailApiParser(IrailStationProvider stationProvider) {
        this.stationProvider = stationProvider;
    }

    public RouteResult parseRouteResult(JSONObject json, Station origin, Station destination, Date lastSearchTime, RouteTimeDefinition timeDefinition) throws JSONException {
        JSONArray routesObject = json.getJSONArray("connection");
        Route[] routes = new Route[routesObject.length()];
        for (int i = 0; i < routesObject.length(); i++) {
            routes[i] = parseRoute(routesObject.getJSONObject(i));
        }
        return new RouteResult(origin, destination, lastSearchTime, timeDefinition, routes);
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
                0,
                false,
                departure.getInt("delay"),
                departure.getInt("canceled") != 0
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
                arrival.getInt("delay"),
                arrival.getInt("canceled") != 0,
                0, false
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

                // don't use parsestop, we have to combine data!
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
                        viaArrival.getInt("delay"),
                        viaArrival.getInt("canceled") != 0,
                        viaDeparture.getInt("delay"),
                        viaDeparture.getInt("canceled") != 0
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
                departure.getInt("delay"), departure.getString("platform"),
                departure.getJSONObject("platforminfo").getInt("normal") == 1,
                timestamp2date(arrival.getString("time")),
                arrival.getInt("delay"),
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

    public LiveBoard parseLiveboard(JSONObject jsonData, Date searchDate) throws JSONException {

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
        return new TrainStop(
                stop,
                destination,
                new TrainStub(item.getString("vehicle"), destination),
                item.getString("platform"),
                item.getJSONObject("platforminfo").getInt("normal") == 1,
                timestamp2date(item.getString("time")),
                item.getInt("delay"),
                item.getInt("canceled") != 0,
                (item.has("left")) && (item.getInt("left") == 1)
        );
    }

    // allow providing station, so liveboards don't need to parse a station over and over
    private TrainStop parseTrainStop(Station destination, TrainStub t, JSONObject item) throws JSONException {
        Station stop = stationProvider.getStationById(item.getJSONObject("stationinfo").getString("id"));
        return new TrainStop(
                stop,
                destination,
                t,
                item.getString("platform"),
                item.getJSONObject("platforminfo").getInt("normal") == 1,
                timestamp2date(item.getString("scheduledDepartureTime")),
                timestamp2date(item.getString("scheduledArrivalTime")),
                item.getInt("departureDelay"),
                item.getInt("arrivalDelay"),
                item.getInt("departureCanceled") != 0,
                item.getInt("arrivalCanceled") != 0,
                false
        );
    }

    public Train parseTrain(JSONObject jsonData, Date searchdate) throws JSONException {

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

        TrainStop lastHaltedStop = null;

        for (int i = 0; i < jsonStops.length(); i++) {
            stops[i] = parseTrainStop(destination, t, jsonStops.getJSONObject(i));
            if (stops[i].getStation().getLatitude() == latitude && stops[i].getStation().getLongitude() == longitude) {
                lastHaltedStop = stops[i];
                for (int j = i; j >=0; j--){
                    stops[j].setHasLeft(true);
                }
            }
        }

        return new Train(id, destination, stops[0].getStation(), longitude, latitude, stops, lastHaltedStop);
    }

    private static Date timestamp2date(String time) {
        return timestamp2date(Long.parseLong(time));
    }

    private static Date timestamp2date(long time) {
        Date date = new Date();
        date.setTime(time * 1000);
        return date;
    }
}
