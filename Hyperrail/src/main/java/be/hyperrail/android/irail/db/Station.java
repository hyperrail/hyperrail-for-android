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

import java.io.Serializable;
import java.util.Date;

import be.hyperrail.android.irail.contracts.IrailDataProvider;
import be.hyperrail.android.irail.contracts.IrailDataResponse;
import be.hyperrail.android.irail.factories.IrailFactory;
import be.hyperrail.android.irail.implementation.LiveBoard;

/**
 * This class represents a station, as found in irail/stationscsv
 * <p>
 * https://github.com/iRail/stations/blob/master/stations.csv
 */
public class Station implements Serializable {

    private final String id;
    private final String name;
    private final String alternative_nl;
    private final String alternative_fr;
    private final String alternative_de;
    private final String alternative_en;
    private final String localizedName;
    private final String country_code;
    private final double latitude;
    private final double longitude;
    private final float avgStopTimes;

    public Station(String id, String name, String nl, String fr, String de, String en, String localizedName, String country, double latitude, double longitude, float avgStopTimes) {
        this.id = id;
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

    public String getName() {
        return name;
    }

    /**
     * Get the liveboard for this station on a certain date
     *
     * @param date the Date and time for which this liveboard should be retreived
     * @return an {@link IrailDataResponse}, containing a {@link LiveBoard} for this station
     */
    public IrailDataResponse<LiveBoard> getLiveBoard(Date date) {
        IrailDataProvider api = IrailFactory.getDataProviderInstance();
        return api.getLiveboard(this.getName(), date);
    }

    /**
     * Get the liveboard for this station
     *
     * @return an {@link IrailDataResponse}, containing a {@link LiveBoard} for this station
     */
    public IrailDataResponse<LiveBoard> getLiveBoard() {
        IrailDataProvider api = IrailFactory.getDataProviderInstance();
        return api.getLiveboard(this.getName());
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getId() {
        return id;
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

}
