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

package be.hyperrail.opentransportdata.irail;

import com.crashlytics.android.Crashlytics;

import java.io.Serializable;
import java.util.Map;

import be.hyperrail.opentransportdata.OpenTransportApi;
import be.hyperrail.opentransportdata.common.models.StopLocation;

/**
 * This class represents a station, as found in irail/stationscsv
 * <p>
 * https://github.com/iRail/stations/blob/master/stations.csv
 */
public class IrailStation implements StopLocation, Serializable, Comparable {

    protected String hafasId;
    protected String name;
    protected String localizedName;
    protected String country_code;
    protected String country_uri;
    protected double latitude;
    protected double longitude;
    protected float avgStopTimes;
    protected Map<String, String> translations;
    private IrailStationFacilities stationFacilities;

    protected IrailStation() {

    }

    public IrailStation(String hafasId, String name, Map<String, String> translations, String localizedName, String country, double latitude, double longitude, float avgStopTimes) {
        if (hafasId.startsWith("BE.NMBS.")) {
            throw new IllegalArgumentException("Station IDs should not start with BE.NMBS!");
        }

        this.hafasId = hafasId;
        this.name = name;
        this.translations = translations;
        this.localizedName = localizedName;
        this.country_code = country;
        this.country_uri = "";
        this.latitude = latitude;
        this.longitude = longitude;
        this.avgStopTimes = avgStopTimes;
    }

    public IrailStation(StopLocation s) {
        copy(s);
    }

    public void copy(StopLocation copy) {
        this.hafasId = copy.getHafasId();
        this.name = copy.getName();
        this.translations = copy.getTranslations();
        this.localizedName = copy.getLocalizedName();
        this.country_code = copy.getCountryCode();
        this.country_uri = copy.getCountryUri();
        this.latitude = copy.getLatitude();
        this.longitude = copy.getLongitude();
        this.avgStopTimes = copy.getAvgStopTimes();
        if (copy instanceof IrailStation) {
            this.stationFacilities = ((IrailStation) copy).stationFacilities;
        }
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
     *
     * @return a 9 digit HAFAS identifier
     */
    public String getHafasId() {
        return hafasId;
    }

    /**
     * Get the 7-digit UIC id for this station
     *
     * @return a 7 digit worldwide unique identifier for this station
     */
    public String getUicId() {
        return hafasId.substring(2);
    }

    public String getUri() {
        return "http://irail.be/stations/NMBS/" + hafasId;
    }

    /**
     * Get the NL, FR, DE or EN name based on the device language
     *
     * @return The NL, FR, DE or EN name based on the device language
     */
    public String getLocalizedName() {
        return localizedName;
    }

    @Override
    public Map<String, String> getTranslations() {
        return translations;
    }

    public String getCountryCode() {
        return country_code;
    }

    @Override
    public String getCountryUri() {
        // TODO: implement
        return "This function is not yet implemented";
    }

    public float getAvgStopTimes() {
        return avgStopTimes;
    }


    /**
     * Get the facilities available in this station.
     * This data is loaded using lazy-loading, do not use this on a large amount of stations
     *
     * @return A StationFacilities object for this station
     */
    public IrailStationFacilities getStationFacilities() {
        if (stationFacilities == null) {
            IrailFacilitiesDataProvider provider = OpenTransportApi.getFacilitiesProviderInstance();
            if (!(provider instanceof IrailFacilitiesDataProvider)) {
                Crashlytics.logException(new IllegalAccessError("Station facilities can only be retrieved through an instance of StationsDB"));
                return null;
            }
            this.stationFacilities = ((IrailFacilitiesDataProvider) provider).getStationFacilitiesByUri(this.hafasId);
        }
        return stationFacilities;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof IrailStation && this.getHafasId().equals(((IrailStation) obj).getHafasId());
    }

    @Override
    public int compareTo(Object o) {
        if (!(o instanceof IrailStation)) {
            return -1;
        }
        return getLocalizedName().compareTo(((IrailStation) o).getLocalizedName());
    }
}
