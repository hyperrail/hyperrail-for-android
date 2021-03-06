package be.hyperrail.opentransportdata.common.models.implementation;

import java.util.Map;

import be.hyperrail.opentransportdata.OpenTransportApi;
import be.hyperrail.opentransportdata.common.models.StopLocation;
import be.hyperrail.opentransportdata.common.models.StopLocationFacilities;

@SuppressWarnings("WeakerAccess")
public class StopLocationImpl implements StopLocation {
    protected String hafasId;
    protected String name;
    protected String localizedName;
    protected String country_code;
    protected String country_uri;
    protected double latitude;
    protected double longitude;
    protected float avgStopTimes;
    protected Map<String, String> translations;
    private String semanticId;
    private StopLocationFacilities stationFacilities;

    public StopLocationImpl(String hafasId, String semanticIdentifier, String name, Map<String, String> translations,
                            String localizedName, String country, double latitude, double longitude, float avgStopTimes) {
        if (hafasId.startsWith("BE.NMBS.")) {
            throw new IllegalArgumentException("Station IDs should not start with BE.NMBS!");
        }

        this.hafasId = hafasId;
        this.semanticId = semanticIdentifier;
        this.name = name;
        this.translations = translations;
        this.localizedName = localizedName;
        this.country_code = country;
        this.country_uri = "";
        this.latitude = latitude;
        this.longitude = longitude;
        this.avgStopTimes = avgStopTimes;
    }

    public StopLocationImpl(StopLocation stopLocation) {
        copy(stopLocation);
    }

    private void copy(StopLocation original) {
        this.hafasId = original.getHafasId();
        this.semanticId = original.getSemanticId();
        this.name = original.getName();
        this.translations = original.getTranslations();
        this.localizedName = original.getLocalizedName();
        this.country_code = original.getCountryCode();
        this.country_uri = original.getCountryUri();
        this.latitude = original.getLatitude();
        this.longitude = original.getLongitude();
        this.avgStopTimes = original.getAvgStopTimes();
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

    public String getSemanticId() {
        return semanticId;
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

    @Override
    public StopLocationFacilities getStationFacilities() {
        // Lazy loading facilities
        if (stationFacilities == null) {
            stationFacilities = OpenTransportApi.getFacilitiesProviderInstance().getStationFacilities(this);
        }
        return stationFacilities;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof StopLocationImpl && this.getHafasId().equals(((StopLocationImpl) obj).getHafasId());
    }

    @Override
    public int compareTo(Object o) {
        if (!(o instanceof StopLocationImpl)) {
            return -1;
        }
        return getLocalizedName().compareTo(((StopLocationImpl) o).getLocalizedName());
    }
}
