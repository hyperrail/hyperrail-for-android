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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
     * @param stopLocations       The list of stations for which a name should be retrieved.
     * @param includeTranslations Whether translations should be included in the name list.
     *                            If this is set to false, only localized names are used.
     * @return An array of localized station names.
     */
    @NonNull
    String[] getStoplocationsNames(@NonNull StopLocation[] stopLocations, boolean includeTranslations);


    /**
     * Get a station by its Hafas ID (This format is similar to the UIC format, but longer and can include bus stops
     * Example custom country code for bus stops: 02
     *
     * @param id a 9 digit ID String
     * @return The station object.
     * @deprecated use getStoplocationBySemanticId instead.
     */
    @Nullable
    @Deprecated
    StopLocation getStoplocationByHafasId(String id) throws StopLocationNotResolvedException;

    /**
     * Get a station by its URI
     *
     * @param uri a uri string
     * @return The station object.
     */
    @Nullable
    StopLocation getStoplocationBySemanticId(String uri) throws StopLocationNotResolvedException;

    /**
     * Get a station by its name.
     *
     * @param name The name of the station to find
     * @return The station object.
     */
    @Nullable
    StopLocation getStoplocationByExactName(String name);

    /**
     * Get stations by their name (or a part thereof), ordered by their size, measured in average train stops per day.
     *
     * @param name The (beginning of) the station name.
     * @return An array of station objects ordered by their size, measured in average train stops per day.
     */
    @NonNull
    StopLocation[] getStoplocationsByNameOrderBySize(String name);

    /**
     * Get stations by their name (or a part thereof), ordered by their distance from a given location
     *
     * @param name     The (beginning of) the station name.
     * @param location The location from which distances should be measured
     * @return An array of station objects ordered by their distance from the given location
     */
    @NonNull
    StopLocation[] getStoplocationsByNameOrderByLocation(String name, Location location);

    /**
     * Get all stations ordered by their distance from a given location
     *
     * @param location The location from which distances should be measured
     * @return An array of all station objects ordered by their distance from the given location
     */
    @NonNull
    StopLocation[] getStoplocationsOrderedByLocation(Location location);

    /**
     * Get all stations ordered by their size, measured in average train stops per day.
     *
     * @return An array of station objects ordered by their size, measured in average train stops per day.
     */
    @NonNull
    StopLocation[] getStoplocationsOrderedBySize();

    /**
     * Get the n closest stations to a location, ordered by their size, measured in average train stops per day.
     *
     * @param limit    The number of stations to return
     * @param location The location from which distance should be measured
     * @return An array of station objects ordered by their size, measured in average train stops per day.
     */
    @NonNull
    StopLocation[] getStoplocationsOrderedByLocationAndSize(Location location, int limit);

    void preloadDatabase();
}
