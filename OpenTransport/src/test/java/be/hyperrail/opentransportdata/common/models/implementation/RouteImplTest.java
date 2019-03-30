package be.hyperrail.opentransportdata.common.models.implementation;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import be.hyperrail.opentransportdata.common.contracts.TransportOccupancyLevel;
import be.hyperrail.opentransportdata.common.models.Message;
import be.hyperrail.opentransportdata.common.models.Route;
import be.hyperrail.opentransportdata.common.models.RouteLeg;
import be.hyperrail.opentransportdata.common.models.RouteLegEnd;
import be.hyperrail.opentransportdata.common.models.RouteLegType;
import be.hyperrail.opentransportdata.common.models.StopLocation;
import be.hyperrail.opentransportdata.common.models.VehicleJourneyStub;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteImplTest {

    @Test
    void getRouteDetails_delayInFirstTransfer_shouldReturnCorrectDetails() {
        VehicleJourneyStub firstLegVehicle = Mockito.mock(VehicleJourneyStub.class);
        VehicleJourneyStub secondLegVehicle = Mockito.mock(VehicleJourneyStub.class);

        StopLocation firstStation = Mockito.mock(StopLocation.class);
        StopLocation secondStation = Mockito.mock(StopLocation.class);
        StopLocation thirdStation = Mockito.mock(StopLocation.class);

        RouteLegEnd firstDeparture = new RouteLegEndImpl(firstStation, new DateTime(0), "2", true,
                Duration.ZERO, false, false, null, TransportOccupancyLevel.UNKNOWN);

        RouteLegEnd firstArrival = new RouteLegEndImpl(secondStation, new DateTime(100 * 1000), "4", true,
                new Duration(60 * 1000), false, false, null, TransportOccupancyLevel.UNKNOWN);

        RouteLegEnd secondDeparture = new RouteLegEndImpl(secondStation, new DateTime(300 * 1000), "B", true,
                Duration.ZERO, false, false, null, TransportOccupancyLevel.UNKNOWN);

        RouteLegEnd secondArrival = new RouteLegEndImpl(thirdStation, new DateTime(900 * 1000), "2A", true,
                Duration.ZERO, false, false, null, TransportOccupancyLevel.UNKNOWN);

        RouteLeg firstLeg = new RouteLegImpl(RouteLegType.TRAIN, firstLegVehicle, firstDeparture, firstArrival);
        RouteLeg secondLeg = new RouteLegImpl(RouteLegType.TRAIN, secondLegVehicle, secondDeparture, secondArrival);

        RouteLeg[] legs = new RouteLeg[]{firstLeg, secondLeg};

        Route route = new RouteImpl(legs);

        assertEquals(1, route.getTransferCount());
        assertEquals(3, route.getTransfers().length);

        assertArrayEquals(legs, route.getLegs());
        assertNull(route.getDeparture().getArrivalPlatform());
        assertEquals("2", route.getDeparturePlatform());
        assertEquals("2", route.getDeparture().getDeparturePlatform());
        assertEquals("4", route.getTransfers()[1].getArrivalPlatform());
        assertEquals("B", route.getTransfers()[1].getDeparturePlatform());
        assertEquals("2A", route.getArrival().getArrivalPlatform());
        assertEquals("2A", route.getArrivalPlatform());
        assertNull(route.getArrival().getDeparturePlatform());

        assertEquals(firstStation, route.getDeparture().getStopLocation());
        assertEquals(firstStation, route.getDepartureStation());
        assertEquals(secondStation, route.getTransfers()[1].getStopLocation());
        assertEquals(thirdStation, route.getArrival().getStopLocation());
        assertEquals(thirdStation, route.getArrivalStation());

        assertEquals(new Duration(900 * 1000), route.getDuration());
        assertEquals(new Duration(900 * 1000), route.getDurationIncludingDelays());

        assertFalse(route.isPartiallyCanceled());
    }

    @Test
    void getRouteDetails_partiallyCanceledAndDelayed_shouldReturnCorrectDetails() {
        VehicleJourneyStub firstLegVehicle = Mockito.mock(VehicleJourneyStub.class);
        VehicleJourneyStub secondLegVehicle = Mockito.mock(VehicleJourneyStub.class);

        StopLocation firstStation = Mockito.mock(StopLocation.class);
        StopLocation secondStation = Mockito.mock(StopLocation.class);
        StopLocation thirdStation = Mockito.mock(StopLocation.class);

        RouteLegEnd firstDeparture = new RouteLegEndImpl(firstStation, new DateTime(0), "2", false,
                Duration.ZERO, false, false, null, TransportOccupancyLevel.UNKNOWN);

        RouteLegEnd firstArrival = new RouteLegEndImpl(secondStation, new DateTime(100 * 1000), "4", true,
                new Duration(60 * 1000), true, false, null, TransportOccupancyLevel.UNKNOWN);

        RouteLegEnd secondDeparture = new RouteLegEndImpl(secondStation, new DateTime(300 * 1000), "B", true,
                Duration.ZERO, false, false, null, TransportOccupancyLevel.UNKNOWN);

        RouteLegEnd secondArrival = new RouteLegEndImpl(thirdStation, new DateTime(900 * 1000), "2A", true,
                new Duration(600 * 1000), false, false, null, TransportOccupancyLevel.UNKNOWN);

        RouteLeg firstLeg = new RouteLegImpl(RouteLegType.TRAIN, firstLegVehicle, firstDeparture, firstArrival);
        RouteLeg secondLeg = new RouteLegImpl(RouteLegType.TRAIN, secondLegVehicle, secondDeparture, secondArrival);

        RouteLeg[] legs = new RouteLeg[]{firstLeg, secondLeg};

        Route route = new RouteImpl(legs);

        assertEquals(1, route.getTransferCount());
        assertEquals(3, route.getTransfers().length);

        assertArrayEquals(legs, route.getLegs());
        assertNull(route.getDeparture().getArrivalPlatform());
        assertEquals("2", route.getDeparturePlatform());
        assertEquals("2", route.getDeparture().getDeparturePlatform());
        assertEquals("4", route.getTransfers()[1].getArrivalPlatform());
        assertEquals("B", route.getTransfers()[1].getDeparturePlatform());
        assertEquals("2A", route.getArrival().getArrivalPlatform());
        assertEquals("2A", route.getArrivalPlatform());
        assertNull(route.getArrival().getDeparturePlatform());

        assertEquals(firstStation, route.getDeparture().getStopLocation());
        assertEquals(secondStation, route.getTransfers()[1].getStopLocation());
        assertEquals(thirdStation, route.getArrival().getStopLocation());

        assertEquals(new Duration(900 * 1000), route.getDuration());
        assertEquals(new Duration(1500 * 1000), route.getDurationIncludingDelays());

        assertTrue(route.isPartiallyCanceled());
        assertFalse(route.getDeparture().isDeparturePlatformNormal());
        assertFalse(route.isDeparturePlatformNormal());
        assertTrue(route.isArrivalDeparturePlatformNormal());
    }

    @Test
    void setAlerts_getAlertsAfterwards_shouldReturnSameContent() {
        VehicleJourneyStub firstLegVehicle = Mockito.mock(VehicleJourneyStub.class);
        VehicleJourneyStub secondLegVehicle = Mockito.mock(VehicleJourneyStub.class);

        StopLocation firstStation = Mockito.mock(StopLocation.class);
        StopLocation secondStation = Mockito.mock(StopLocation.class);
        StopLocation thirdStation = Mockito.mock(StopLocation.class);

        RouteLegEnd firstDeparture = new RouteLegEndImpl(firstStation, new DateTime(0), "2", true,
                Duration.ZERO, false, false, null, TransportOccupancyLevel.UNKNOWN);

        RouteLegEnd firstArrival = new RouteLegEndImpl(secondStation, new DateTime(100 * 1000), "4", true,
                new Duration(60 * 1000), false, false, null, TransportOccupancyLevel.UNKNOWN);

        RouteLegEnd secondDeparture = new RouteLegEndImpl(secondStation, new DateTime(300 * 1000), "B", true,
                Duration.ZERO, false, false, null, TransportOccupancyLevel.UNKNOWN);

        RouteLegEnd secondArrival = new RouteLegEndImpl(thirdStation, new DateTime(900 * 1000), "2A", true,
                Duration.ZERO, false, false, null, TransportOccupancyLevel.UNKNOWN);

        RouteLeg firstLeg = new RouteLegImpl(RouteLegType.TRAIN, firstLegVehicle, firstDeparture, firstArrival);
        RouteLeg secondLeg = new RouteLegImpl(RouteLegType.TRAIN, secondLegVehicle, secondDeparture, secondArrival);

        RouteLeg[] legs = new RouteLeg[]{firstLeg, secondLeg};

        Route route = new RouteImpl(legs);

        Message[] alerts = new Message[]{Mockito.mock(Message.class)};
        Message[] remarks = new Message[]{Mockito.mock(Message.class)};
        Message[][] vehicleAlerts = new Message[][]{new Message[]{Mockito.mock(Message.class)}};

        route.setAlerts(alerts);
        assertArrayEquals(alerts, route.getAlerts());

        route.setRemarks(remarks);
        assertArrayEquals(remarks, route.getRemarks());

        route.setVehicleAlerts(vehicleAlerts);
        assertArrayEquals(vehicleAlerts, route.getVehicleAlerts());
    }

}