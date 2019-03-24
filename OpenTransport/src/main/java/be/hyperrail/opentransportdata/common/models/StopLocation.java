/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.opentransportdata.common.models;

import java.io.Serializable;
import java.util.Map;

/**
 * This class represents a station, as found in irail/stationscsv
 * <p>
 * https://github.com/iRail/stations/blob/master/stations.csv
 */
public interface StopLocation extends Serializable, Comparable {

    /**
     * The local name of this stop
     * @return
     */
    String getName();

    /**
     * The latitude of this stop
     * @return
     */
    double getLatitude();

    /**
     * The longitude of this stop
     * @return
     */
    double getLongitude();

    /**
     * Get the 9-digit HAFAS Id for this station
     *
     * @return a 9 digit HAFAS identifier
     * @deprecated use getUri instead
     */
    String getHafasId();

    /**
     * Get the 7-digit UIC id for this station
     *
     * @return a 7 digit worldwide unique identifier for this station
     * @deprecated use getUri instead
     */
    String getUicId();

    /**
     * Get the URI identifying this stop
     *
     * @return
     */
    String getUri();

    /**
     * Get the NL, FR, DE or EN name based on the device language
     *
     * @return The NL, FR, DE or EN name based on the device language
     */
    String getLocalizedName();

    /**
     * Get the map of translations for this stop location's name
     * @return
     */
    Map<String,String> getTranslations();

    /**
     * Get the country code representing the country this stop is in
     *
     * @return
     */
    String getCountryCode();

    /**
     * Get the country URI representing the country this stop is in
     *
     * @return
     */
    String getCountryUri();

    /**
     * Get the average number of vehicle stops at this stop
     *
     * @return
     */
    float getAvgStopTimes();

    StopLocationFacilities getStationFacilities();
}
