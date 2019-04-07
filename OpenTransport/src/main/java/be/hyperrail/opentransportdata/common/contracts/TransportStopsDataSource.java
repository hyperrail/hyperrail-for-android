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

package be.hyperrail.opentransportdata.common.contracts;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import be.hyperrail.opentransportdata.common.exceptions.StopLocationNotResolvedException;
import be.hyperrail.opentransportdata.common.models.StopLocation;

/**
 * A station provider, returning stations from irail/stationscsv or a datasource with similar fields.
 * See http://docs.irail.be
 */
public interface TransportStopsDataSource {

    /**
     * Get all station names, localized.
     *
     * @param Stations The list of stations for which a name should be retrieved.
     * @return An array of localized station names.
     */
    @NonNull
    String[] getStationNames(@NonNull StopLocation[] Stations);

    /**
     * Get a station by its UIC ID (International Railway Station ID)
     *
     * @param id a 7 digit ID string
     * @return The station object, null if not found
     */
    @Nullable
    StopLocation getStationByUIC(String id) throws StopLocationNotResolvedException;

    /**
     * Get a station by its Hafas ID (This format is similar to the UIC format, but longer and can include bus stops
     * Example custom country code for bus stops: 02
     *
     * @param id a 9 digit ID String
     * @return The station object.
     */
    @Nullable
    StopLocation getStationByHID(String id) throws StopLocationNotResolvedException;

    /**
     * Get a station by its ID
     *
     * @param id an ID string, in BE.NMBS.XXXXXXXX or Hafas ID format
     * @return The station object.
     */
    @Deprecated
    @Nullable
    StopLocation getStationByIrailApiId(String id) throws StopLocationNotResolvedException;

    /**
     * Get a station by its URI
     *
     * @param uri a uri string
     * @return The station object.
     */
    @Nullable
    StopLocation getStationByUri(String uri) throws StopLocationNotResolvedException;

    /**
     * Get a station by its name.
     *
     * @param name The name of the station to find
     * @return The station object.
     */
    @Nullable
    StopLocation getStationByExactName(String name);

    /**
     * Get stations by their name (or a part thereof), ordered by their size, measured in average train stops per day.
     *
     * @param name The (beginning of) the station name.
     * @return An array of station objects ordered by their size, measured in average train stops per day.
     */
    @NonNull
    StopLocation[] getStationsByNameOrderBySize(String name);

    /**
     * Get stations by their name (or a part thereof), ordered by their distance from a given location
     *
     * @param name     The (beginning of) the station name.
     * @param location The location from which distances should be measured
     * @return An array of station objects ordered by their distance from the given location
     */
    @NonNull
    StopLocation[] getStationsByNameOrderByLocation(String name, Location location);

    /**
     * Get all stations ordered by their distance from a given location
     *
     * @param location The location from which distances should be measured
     * @return An array of all station objects ordered by their distance from the given location
     */
    @NonNull
    StopLocation[] getStationsOrderByLocation(Location location);

    /**
     * Get all stations ordered by their size, measured in average train stops per day.
     *
     * @return An array of station objects ordered by their size, measured in average train stops per day.
     */
    @NonNull
    StopLocation[] getStationsOrderBySize();

    /**
     * Get the n closest stations to a location, ordered by their size, measured in average train stops per day.
     *
     * @param limit    The number of stations to return
     * @param location The location from which distance should be measured
     * @return An array of station objects ordered by their size, measured in average train stops per day.
     */
    @NonNull
    StopLocation[] getStationsOrderByLocationAndSize(Location location, int limit);
}
