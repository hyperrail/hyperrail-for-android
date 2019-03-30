package be.hyperrail.opentransportdata.common.models.implementation;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import be.hyperrail.opentransportdata.common.contracts.TransportOccupancyLevel;
import be.hyperrail.opentransportdata.common.models.StopLocation;
import be.hyperrail.opentransportdata.common.models.VehicleStop;
import be.hyperrail.opentransportdata.common.models.VehicleStopType;
import be.hyperrail.opentransportdata.common.models.VehicleJourneyStub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleJourneyStopImplTest {

    @Test
    void createInstance_callGetters_shouldReturnCorrectValues() {
        StopLocation stopLocation = Mockito.mock(StopLocationImpl.class);
        VehicleJourneyStub vehicleJourneyStub = Mockito.mock(VehicleJourneyStub.class);
        DateTime arrivalTime = DateTime.now();
        DateTime departureTime = DateTime.now().plusMinutes(3);
        VehicleStop vehicleStop = new VehicleStopImpl(stopLocation, vehicleJourneyStub,
                "A", true, departureTime, arrivalTime, Duration.standardMinutes(2), Duration.standardSeconds(60), false, false,
                false, "http://mock/uri", TransportOccupancyLevel.UNKNOWN, VehicleStopType.STOP);

        assertEquals(stopLocation, vehicleStop.getStopLocation());
        assertEquals(vehicleJourneyStub, vehicleStop.getVehicle());
        assertEquals(Duration.standardMinutes(1), vehicleStop.getArrivalDelay());
        assertEquals(Duration.standardMinutes(2), vehicleStop.getDepartureDelay());
        assertEquals(arrivalTime, vehicleStop.getArrivalTime());
        assertEquals(departureTime, vehicleStop.getDepartureTime());
        assertEquals(arrivalTime.plusMinutes(1), vehicleStop.getDelayedArrivalTime());
        assertEquals(departureTime.plusMinutes(2), vehicleStop.getDelayedDepartureTime());
        assertEquals("A", vehicleStop.getPlatform());
        assertTrue(vehicleStop.isPlatformNormal());
        assertFalse(vehicleStop.isDepartureCanceled());
        assertFalse(vehicleStop.isArrivalCanceled());
        assertFalse(vehicleStop.hasLeft());
        assertEquals("http://mock/uri", vehicleStop.getDepartureUri());
        assertEquals(TransportOccupancyLevel.UNKNOWN, vehicleStop.getOccupancyLevel());
        assertEquals(VehicleStopType.STOP, vehicleStop.getType());
    }

}