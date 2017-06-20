/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package android.irail.be.hyperrail.irail.db;

import android.irail.be.hyperrail.irail.contracts.IrailDataProvider;
import android.irail.be.hyperrail.irail.contracts.IrailDataResponse;
import android.irail.be.hyperrail.irail.factories.IrailFactory;
import android.irail.be.hyperrail.irail.implementation.LiveBoard;

import java.io.Serializable;
import java.util.Date;
import java.util.Locale;

/**
 * This class represents a station, as found in irail/stationscsv
 *
 * https://github.com/iRail/stations/blob/master/stations.csv
 */
public class Station implements Serializable {
    private String id;
    private String name;
    private String alternative_nl;
    private String alternative_fr;
    private String alternative_de;
    private String alternative_en;
    private String country_code;
    private double latitude;
    private double longitude;
    private float avgStopTimes;

    public Station(String id, String name, String nl, String fr, String de, String en, String country, double latitude, double longitude, float avgStopTimes) {
        this.id = id;
        this.name = name;
        this.alternative_nl = nl;
        this.alternative_fr = fr;
        this.alternative_en = en;
        this.alternative_de = de;
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
     * @param date the Date and time for which this liveboard should be retreived
     * @return an {@link IrailDataResponse}, containing a {@link LiveBoard} for this station
     */
    public IrailDataResponse<LiveBoard> getLiveBoard(Date date)  {
        IrailDataProvider api = IrailFactory.getDataProviderInstance();
        return api.getLiveboard(this.getName(), date);
    }
    /**
     * Get the liveboard for this station
     * @return an {@link IrailDataResponse}, containing a {@link LiveBoard} for this station
     */
    public IrailDataResponse<LiveBoard> getLiveBoard()  {
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

    /**
     * Get the NL, FR, DE or EN name based on the device language
     * @return The NL, FR, DE or EN name based on the device language
     * @TODO device language should be set as a setting (one time), after which user can choose in settings
     */
    public String getLocalizedName() {
        switch (Locale.getDefault().getISO3Language()) {
            case "nld":
                if (getAlternativeNl() != null && !getAlternativeNl().isEmpty()) {
                    return getAlternativeNl();
                } else {
                    return getName();
                }
            case "fra":
                if (getAlternativeFr() != null && !getAlternativeFr().isEmpty()) {
                    return getAlternativeFr();
                } else {
                    return getName();
                }
            case "deu":
                if (getAlternativeDe() != null && !getAlternativeDe().isEmpty()) {
                    return getAlternativeDe();
                } else {
                    return getName();
                }
            case "eng":
                if (getAlternativeEn() != null && !getAlternativeEn().isEmpty()) {
                    return getAlternativeEn();
                } else {
                    return getName();
                }
            default:
                return getName();
        }
    }

    public String getCountryCode() {
        return country_code;
    }

    public float getAvgStopTimes() {
        return avgStopTimes;
    }

}
