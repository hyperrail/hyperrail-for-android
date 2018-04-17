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

package be.hyperrail.android.irail.db;

import android.support.annotation.NonNull;

import com.google.firebase.crash.FirebaseCrash;

import java.io.Serializable;

import be.hyperrail.android.irail.contracts.IrailStationProvider;
import be.hyperrail.android.irail.factories.IrailFactory;

/**
 * This class represents a station, as found in irail/stationscsv
 * <p>
 * https://github.com/iRail/stations/blob/master/stations.csv
 */
public class Station implements Serializable, Comparable {

    protected String hafasId;
    protected String name;
    protected String alternative_nl;
    protected String alternative_fr;
    protected String alternative_de;
    protected String alternative_en;
    protected String localizedName;
    protected String country_code;
    protected double latitude;
    protected double longitude;
    protected float avgStopTimes;

    protected Station() {

    }

    public Station(String hafasId, String name, String nl, String fr, String de, String en, String localizedName, String country, double latitude, double longitude, float avgStopTimes) {
        if (hafasId.startsWith("BE.NMBS.")) {
            throw new IllegalArgumentException("Station IDs should not start with BE.NMBS!");
        }

        this.hafasId = hafasId;
        this.name = name;
        this.alternative_nl = nl;
        this.alternative_fr = fr;
        this.alternative_en = en;
        this.alternative_de = de;
        this.localizedName = localizedName;
        this.country_code = country;
        this.latitude = latitude;
        this.longitude = longitude;
        this.avgStopTimes = avgStopTimes;
    }

    public Station(Station s) {
        copy(s);
    }

    public void copy(Station copy) {
        this.hafasId = copy.hafasId;
        this.name = copy.name;
        this.alternative_nl = copy.alternative_nl;
        this.alternative_fr = copy.alternative_fr;
        this.alternative_en = copy.alternative_en;
        this.alternative_de = copy.alternative_de;
        this.localizedName = copy.localizedName;
        this.country_code = copy.country_code;
        this.latitude = copy.latitude;
        this.longitude = copy.longitude;
        this.avgStopTimes = copy.avgStopTimes;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    /**
     * Get the 9-digit HAFAS Id for this station
     * @return a 9 digit HAFAS identifier
     *
     */
    public String getHafasId() {
        return hafasId;
    }

    /**
     * Get the 7-digit UIC id for this station
     * @return a 7 digit worldwide unique identifier for this station
     */
    public String getUicId() {
        return hafasId.substring(2);
    }

    public String getUri() {
        return "http://irail.be/stations/NMBS/" + hafasId;
    }

    public String getAlternativeNl() {
        return alternative_nl;
    }

    public String getAlternativeFr() {
        return alternative_fr;
    }

    public String getAlternativeDe() {
        return alternative_de;
    }

    public String getAlternativeEn() {
        return alternative_en;
    }

    // @TODO device language should be set as a setting (one time), after which user can choose in settings

    /**
     * Get the NL, FR, DE or EN name based on the device language
     *
     * @return The NL, FR, DE or EN name based on the device language
     */
    public String getLocalizedName() {
        return localizedName;
    }

    public String getCountryCode() {
        return country_code;
    }

    public float getAvgStopTimes() {
        return avgStopTimes;
    }

    private StationFacilities stationFacilities;

    /**
     * Get the facilities available in this station.
     * This data is loaded using lazy-loading, do not use this on a large amount of stations
     *
     * @return A StationFacilities object for this station
     */
    public StationFacilities getStationFacilities() {
        if (stationFacilities == null) {
            IrailStationProvider provider = IrailFactory.getStationsProviderInstance();
            if (!(provider instanceof StationsDb)) {
                FirebaseCrash.report(new IllegalAccessError("Station facilities can only be retrieved through an instance of StationsDB"));
                return null;
            }
            this.stationFacilities = ((StationsDb) provider).getStationFacilitiesById(this.hafasId);
        }
        return stationFacilities;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Station && this.getHafasId().equals(((Station) obj).getHafasId());
    }

    @Override
    public int compareTo(@NonNull Object o) {
        if (!(o instanceof Station)) {
            return -1;
        }
        return getLocalizedName().compareTo(((Station) o).getLocalizedName());
    }
}
