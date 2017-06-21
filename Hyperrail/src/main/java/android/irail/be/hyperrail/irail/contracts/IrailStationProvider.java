/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package android.irail.be.hyperrail.irail.contracts;

import android.irail.be.hyperrail.irail.db.Station;
import android.location.Location;

/**
 * A station provider, returning stations from irail/stationscsv or a datasource with similar fields.
 * See http://docs.irail.be
 */
public interface IrailStationProvider {

    /**
     * Get all station names, localized.
     *
     * @param Stations The list of stations for which a name should be retrieved.
     * @return An array of localized station names.
     */
    String[] getStationNames(Station[] Stations);

    /**
     * Get a station by its ID
     *
     * @param id an ID string, in BE.NMBS.XXXXXXXX format
     * @return The station object.
     */
    Station getStationById(String id);

    /**
     * Get a station by its name.
     *
     * @param name The name of the station to find
     * @return The station object.
     */
    Station getStationByName(String name);

    /**
     * Get stations by their name (or a part thereof), ordered by their size, measured in average train stops per day.
     *
     * @param name The (beginning of) the station name.
     * @return An array of station objects ordered by their size, measured in average train stops per day.
     */
    Station[] getStationsByNameOrderBySize(String name);

    /**
     * Get stations by their name (or a part thereof), ordered by their distance from a given location
     *
     * @param name     The (beginning of) the station name.
     * @param location The location from which distances should be measured
     * @return An array of station objects ordered by their distance from the given location
     */
    Station[] getStationsByNameOrderByLocation(String name, Location location);

    /**
     * Get all stations ordered by their distance from a given location
     *
     * @param location The location from which distances should be measured
     * @return An array of all station objects ordered by their distance from the given location
     */
    Station[] getStationsOrderByLocation(Location location);

    /**
     * Get all stations ordered by their size, measured in average train stops per day.
     *
     * @return An array of station objects ordered by their size, measured in average train stops per day.
     */
    Station[] getStationsOrderBySize();

    /**
     * Get the n closest stations to a location, ordered by their size, measured in average train stops per day.
     *
     * @param limit    The number of stations to return
     * @param location The location from which distance should be measured
     * @return An array of station objects ordered by their size, measured in average train stops per day.
     */
    Station[] getStationsOrderByLocationAndSize(Location location, int limit);
}
