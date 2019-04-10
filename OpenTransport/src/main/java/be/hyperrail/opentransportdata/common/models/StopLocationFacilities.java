package be.hyperrail.opentransportdata.common.models;

import org.joda.time.LocalTime;

import java.io.Serializable;

public interface StopLocationFacilities extends Serializable {
    /**
     * Return the opening hours for a certain day
     * @param day The day, where 0 is monday and 6 is sunday
     * @return Array where index 0 points to the opening time, and index 1 points to the closing time. Null if no data is available.
     */
    LocalTime[] getOpeningHours(int day);

    String getStreet();

    String getZip();

    String getCity();

    boolean hasTicketVendingMachines();

    boolean hasLuggageLockers();

    boolean hasFreeParking();

    boolean hasBlueBike();

    boolean hasBike();

    boolean hasTaxi();

    boolean hasBus();

    boolean hasTram();

    boolean hasMetro();

    boolean isWheelchairAvailable();

    boolean hasRamp();

    boolean isElevatedPlatform();

    boolean hasEscalatorUp();

    boolean hasEscalatorDown();

    boolean hasElevatorToPlatform();

    boolean hasHearingAidSignal();

    int getDisabledParkingSpots();
}
