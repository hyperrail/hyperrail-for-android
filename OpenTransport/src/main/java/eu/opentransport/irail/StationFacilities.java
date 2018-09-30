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

package eu.opentransport.irail;

import org.joda.time.LocalTime;

import java.io.Serializable;

/**
 * Describes facilities in a station, including transfers, facilities for disabled persons, and opening hours
 */
public class StationFacilities implements Serializable {

    private LocalTime[][] openingHours;
    private String street;
    private String zip;
    private String city;
    private boolean ticketVendingMachines;
    private boolean luggageLockers;
    private boolean freeParking;
    private boolean blue_bike;
    private boolean bike;
    private boolean taxi;
    private boolean bus;
    private boolean tram;
    private boolean metro;
    private boolean wheelchair_available;
    private boolean ramp;
    private boolean elevated_platform;
    private boolean escalator_up;
    private boolean escalator_down;
    private boolean elevator_platform;
    private boolean hearing_aid_signal;
    private int disabled_parking_spots;

    public StationFacilities(LocalTime[][] openingHours, String street, String zip, String city, boolean ticketVendingMachines,
                             boolean luggageLockers, boolean freeParking, boolean blue_bike, boolean bike, boolean taxi, boolean bus,
                             boolean tram, boolean metro, boolean wheelchair_available, boolean ramp, int disabled_parking_spots, boolean elevated_platform,
                             boolean escalator_up, boolean escalator_down, boolean elevator_platform, boolean hearing_aid_signal
    ) {
        this.openingHours = openingHours;
        this.street = street;
        this.zip = zip;
        this.city = city;
        this.ticketVendingMachines = ticketVendingMachines;
        this.luggageLockers = luggageLockers;
        this.freeParking = freeParking;
        this.blue_bike = blue_bike;
        this.bike = bike;
        this.taxi = taxi;
        this.bus = bus;
        this.tram = tram;
        this.metro = metro;
        this.wheelchair_available = wheelchair_available;
        this.ramp = ramp;
        this.elevated_platform = elevated_platform;
        this.escalator_up = escalator_up;
        this.escalator_down = escalator_down;
        this.elevator_platform = elevator_platform;
        this.hearing_aid_signal = hearing_aid_signal;
        this.disabled_parking_spots = disabled_parking_spots;
    }

    /**
     * Return the opening hours for a certain day
     * @param day The day, where 0 is monday and 6 is sunday
     * @return Array where index 0 points to the opening time, and index 1 points to the closing time. Null if no data is available.
     */
    public LocalTime[] getOpeningHours(int day) {
        return openingHours[day];
    }

    public String getStreet() {
        return street;
    }

    public String getZip() {
        return zip;
    }

    public String getCity() {
        return city;
    }

    public boolean hasTicketVendingMachines() {
        return ticketVendingMachines;
    }

    public boolean hasLuggageLockers() {
        return luggageLockers;
    }

    public boolean hasFreeParking() {
        return freeParking;
    }

    public boolean hasBlue_bike() {
        return blue_bike;
    }

    public boolean hasBike() {
        return bike;
    }

    public boolean hasTaxi() {
        return taxi;
    }

    public boolean hasBus() {
        return bus;
    }

    public boolean hasTram() {
        return tram;
    }

    public boolean hasMetro() {
        return metro;
    }

    public boolean isWheelchair_available() {
        return wheelchair_available;
    }

    public boolean hasRamp() {
        return ramp;
    }

    public boolean isElevated_platform() {
        return elevated_platform;
    }

    public boolean hasEscalator_up() {
        return escalator_up;
    }

    public boolean hasEscalator_down() {
        return escalator_down;
    }

    public boolean hasElevator_platform() {
        return elevator_platform;
    }

    public boolean hasHearing_aid_signal() {
        return hearing_aid_signal;
    }

    public int getDisabled_parking_spots() {
        return disabled_parking_spots;
    }
}
