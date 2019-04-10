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

package be.hyperrail.opentransportdata.common.models.implementation;

import org.joda.time.LocalTime;

import java.io.Serializable;

import be.hyperrail.opentransportdata.common.models.StopLocationFacilities;

/**
 * Describes facilities in a station, including transfers, facilities for disabled persons, and opening hours
 */
public class StopLocationFacilitiesImpl implements StopLocationFacilities, Serializable {

    private LocalTime[][] openingHours;
    private String street;
    private String zip;
    private String city;
    private boolean ticketVendingMachines;
    private boolean luggageLockers;
    private boolean freeParking;
    private boolean blueBike;
    private boolean bike;
    private boolean taxi;
    private boolean bus;
    private boolean tram;
    private boolean metro;
    private boolean wheelchairAvailable;
    private boolean ramp;
    private boolean elevatedPlatform;
    private boolean escalatorUp;
    private boolean escalatorDown;
    private boolean elevatorPlatform;
    private boolean hearingAidSignal;
    private int disabledParkingSpots;

    public StopLocationFacilitiesImpl(LocalTime[][] openingHours, String street, String zip, String city, boolean ticketVendingMachines,
                                      boolean luggageLockers, boolean freeParking, boolean blueBike, boolean bike, boolean taxi, boolean bus,
                                      boolean tram, boolean metro, boolean wheelchairAvailable, boolean ramp, int disabledParkingSpots, boolean elevatedPlatform,
                                      boolean escalatorUp, boolean escalatorDown, boolean elevatorPlatform, boolean hearingAidSignal
    ) {
        this.openingHours = openingHours;
        this.street = street;
        this.zip = zip;
        this.city = city;
        this.ticketVendingMachines = ticketVendingMachines;
        this.luggageLockers = luggageLockers;
        this.freeParking = freeParking;
        this.blueBike = blueBike;
        this.bike = bike;
        this.taxi = taxi;
        this.bus = bus;
        this.tram = tram;
        this.metro = metro;
        this.wheelchairAvailable = wheelchairAvailable;
        this.ramp = ramp;
        this.elevatedPlatform = elevatedPlatform;
        this.escalatorUp = escalatorUp;
        this.escalatorDown = escalatorDown;
        this.elevatorPlatform = elevatorPlatform;
        this.hearingAidSignal = hearingAidSignal;
        this.disabledParkingSpots = disabledParkingSpots;
    }

    /**
     * Return the opening hours for a certain day
     * @param day The day, where 0 is monday and 6 is sunday
     * @return Array where index 0 points to the opening time, and index 1 points to the closing time. Null if no data is available.
     */
    @Override
    public LocalTime[] getOpeningHours(int day) {
        return openingHours[day];
    }

    @Override
    public String getStreet() {
        return street;
    }

    @Override
    public String getZip() {
        return zip;
    }

    @Override
    public String getCity() {
        return city;
    }

    @Override
    public boolean hasTicketVendingMachines() {
        return ticketVendingMachines;
    }

    @Override
    public boolean hasLuggageLockers() {
        return luggageLockers;
    }

    @Override
    public boolean hasFreeParking() {
        return freeParking;
    }

    @Override
    public boolean hasBlueBike() {
        return blueBike;
    }

    @Override
    public boolean hasBike() {
        return bike;
    }

    @Override
    public boolean hasTaxi() {
        return taxi;
    }

    @Override
    public boolean hasBus() {
        return bus;
    }

    @Override
    public boolean hasTram() {
        return tram;
    }

    @Override
    public boolean hasMetro() {
        return metro;
    }

    @Override
    public boolean isWheelchairAvailable() {
        return wheelchairAvailable;
    }

    @Override
    public boolean hasRamp() {
        return ramp;
    }

    @Override
    public boolean isElevatedPlatform() {
        return elevatedPlatform;
    }

    @Override
    public boolean hasEscalatorUp() {
        return escalatorUp;
    }

    @Override
    public boolean hasEscalatorDown() {
        return escalatorDown;
    }

    @Override
    public boolean hasElevatorToPlatform() {
        return elevatorPlatform;
    }

    @Override
    public boolean hasHearingAidSignal() {
        return hearingAidSignal;
    }

    @Override
    public int getDisabledParkingSpots() {
        return disabledParkingSpots;
    }
}
