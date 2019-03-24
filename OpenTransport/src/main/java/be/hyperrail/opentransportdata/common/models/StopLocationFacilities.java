package be.hyperrail.opentransportdata.common.models;

import org.joda.time.LocalTime;

public interface StopLocationFacilities {
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

    boolean hasBlue_bike();

    boolean hasBike();

    boolean hasTaxi();

    boolean hasBus();

    boolean hasTram();

    boolean hasMetro();

    boolean isWheelchair_available();

    boolean hasRamp();

    boolean isElevated_platform();

    boolean hasEscalator_up();

    boolean hasEscalator_down();

    boolean hasElevator_platform();

    boolean hasHearing_aid_signal();

    int getDisabled_parking_spots();
}
