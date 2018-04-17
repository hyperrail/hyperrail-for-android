/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package be.hyperrail.android.test;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import be.hyperrail.android.irail.contracts.IrailStationProvider;
import be.hyperrail.android.irail.contracts.OccupancyLevel;
import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.db.StationsDb;
import be.hyperrail.android.irail.implementation.IrailApiParser;
import be.hyperrail.android.irail.implementation.Liveboard;
import be.hyperrail.android.irail.implementation.Route;
import be.hyperrail.android.irail.implementation.RouteResult;
import be.hyperrail.android.irail.implementation.Vehicle;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class IrailParserInstrumentedTest {

    IrailApiParser parser = new IrailApiParser(new StationsDb(InstrumentationRegistry.getTargetContext()));

    @Test
    public void liveboardParsingTest() throws Exception {
        // Context of the app under test.
        DateTime searchTime = new DateTime(2017, 11, 16, 13, 0);
        Liveboard liveboard = parser.parseLiveboard(new JSONObject(LIVEBOARD_RESPONSE), searchTime, Liveboard.LiveboardType.DEPARTURES,RouteTimeDefinition.DEPART_AT);

        assertEquals(searchTime, liveboard.getSearchTime());
        assertEquals("008892007", liveboard.getHafasId());

        assertEquals(31, liveboard.getStops().length);

        // START tests stop 0
        assertEquals("008892007", liveboard.getStops()[0].getStation().getHafasId());
        assertEquals("008892338", liveboard.getStops()[0].getDestination().getHafasId());
        assertEquals(new Duration(1860 * 1000), liveboard.getStops()[0].getDepartureDelay());
        assertEquals(new DateTime((long) 1510833300 * 1000).withZone(DateTimeZone.forID("Europe/Brussels")), liveboard.getStops()[0].getDepartureTime());
        assertEquals("IC3634", liveboard.getStops()[0].getVehicle().getId());

        assertEquals("6", liveboard.getStops()[0].getPlatform());
        assertEquals(false, liveboard.getStops()[0].isPlatformNormal());
        assertEquals(false, liveboard.getStops()[0].hasLeft());

        // URI's should be parsed, not computed. This unique URI will test this.
        assertEquals("http://irail.be/vehicle/TESTPARSEURI", liveboard.getStops()[0].getVehicle().getSemanticId());
        assertEquals(OccupancyLevel.UNKNOWN, liveboard.getStops()[0].getOccupancyLevel());
        // END tests stop 0
        // START tests stop 1
        assertEquals("008892007", liveboard.getStops()[1].getStation().getHafasId());
        assertEquals("008892908", liveboard.getStops()[1].getDestination().getHafasId());
        assertEquals(new Duration(60 * 1000), liveboard.getStops()[1].getDepartureDelay());
        assertEquals(new DateTime((long) 1510833600 * 1000), liveboard.getStops()[1].getDepartureTime().withZone(DateTimeZone.UTC));
        assertEquals("L783", liveboard.getStops()[1].getVehicle().getId());

        assertEquals("4", liveboard.getStops()[1].getPlatform());
        assertEquals(true, liveboard.getStops()[1].isPlatformNormal());
        assertEquals(false, liveboard.getStops()[1].hasLeft());

        assertEquals("http://irail.be/vehicle/L783", liveboard.getStops()[1].getVehicle().getSemanticId());
        assertEquals("http://irail.be/connections/8892007/20171116/DEPARTURECONNECTIONTEST", liveboard.getStops()[1].getDepartureSemanticId());
        assertEquals(OccupancyLevel.HIGH, liveboard.getStops()[1].getOccupancyLevel());
        // END tests stop 1
    }

    @Test
    public void trainParsingTest() throws Exception {
        // Context of the app under test.
        DateTime searchTime = new DateTime(2017, 11, 16, 13, 0);
        Vehicle train = parser.parseTrain(new JSONObject(TRAIN_RESPONSE), searchTime);

        assertEquals("008841004", train.getLastHaltedStop().getStation().getHafasId());
        assertEquals(train.getLastHaltedStop().getStation().getLatitude(), train.getLatitude(), 0);
        assertEquals(train.getLastHaltedStop().getStation().getLongitude(), train.getLongitude(), 0);

        assertEquals("008844628", train.getOrigin().getHafasId());
        assertEquals("Oostende", train.getHeadsign());
        assertEquals("IC537", train.getId());
        assertEquals("http://irail.be/vehicle/PARSETHISVALUE", train.getSemanticId());

        assertEquals(11, train.getStops().length);
        assertEquals(2, train.getStopNumberForStation(new StationsDb(InstrumentationRegistry.getTargetContext()).getStationByHID("008844008")));

        // Start testing stop 2
        assertEquals("008844008", train.getStops()[2].getStation().getHafasId());
        assertEquals(new DateTime((long) 1510839480 * 1000), train.getStops()[2].getArrivalTime().withZone(DateTimeZone.UTC));
        assertEquals(new Duration(120 * 1000), train.getStops()[2].getArrivalDelay());
        assertEquals(new DateTime((long) 1510839540 * 1000), train.getStops()[2].getDepartureTime().withZone(DateTimeZone.UTC));
        assertEquals(new Duration(60 * 1000), train.getStops()[2].getDepartureDelay());
        assertEquals(true, train.getStops()[2].hasLeft());
        assertEquals("http://irail.be/connections/8844008/20171116/IC537", train.getStops()[2].getDepartureSemanticId());
    }

    @Test
    public void routeParsingTest() throws Exception {
        // Context of the app under test.
        IrailStationProvider stationProvider = new StationsDb(InstrumentationRegistry.getTargetContext());
        DateTime searchTime = new DateTime(2017, 11, 16, 14, 0);
        RouteResult routes = parser.parseRouteResult(new JSONObject(ROUTE_RESPONSE), stationProvider.getStationByHID("008893120"),
                                                     stationProvider.getStationByHID("008832375"), searchTime, RouteTimeDefinition.DEPART_AT);

        assertEquals(6, routes.getRoutes().length);
        Route route = routes.getRoutes()[0];

        assertEquals(new Duration(8340 * 1000), route.getDuration());

        assertEquals("008893120", route.getDepartureStation().getHafasId());
        assertEquals(new Duration(120 * 1000), route.getDepartureDelay());
        assertEquals(new DateTime((long) 1510838640 * 1000), route.getDepartureTime().withZone(DateTimeZone.UTC));

        assertEquals("008832375", route.getArrivalStation().getHafasId());
        assertEquals(new Duration(0), route.getArrivalDelay());
        assertEquals(new DateTime((long) 1510838640 * 1000), route.getDepartureTime().withZone(DateTimeZone.UTC));

        assertEquals(1, route.getAlerts().length);
        assertEquals("Probleem bovenleiding  MIVB", route.getAlerts()[0].getHeader());
        assertEquals("Reizigers tussen Brussel-Zuid en Brussel-Noord mogen met hun MIVB-ticket gebruik maken van de treinen van NMBS.", route.getAlerts()[0].getDescription());
        assertEquals("Probleem bovenleiding  MIVB", route.getAlerts()[0].getLead());

        assertArrayEquals(null, route.getVehicleAlerts()[0]);
        assertEquals(1, route.getVehicleAlerts()[1].length);
        assertEquals("Probleem bovenleiding  MIVB", route.getVehicleAlerts()[1][0].getHeader());
        assertEquals("Reizigers tussen Brussel-Zuid en Brussel-Noord mogen met hun MIVB-ticket gebruik maken van de treinen van NMBS.", route.getVehicleAlerts()[1][0].getDescription());
        assertEquals("Probleem bovenleiding  MIVB", route.getVehicleAlerts()[1][0].getLead());

        assertEquals(1, route.getTransferCount());
        assertEquals("008892007", route.getTransfers()[1].getStation().getHafasId());

        assertEquals("4", route.getTransfers()[1].getArrivalPlatform());
        assertEquals(true, route.getTransfers()[1].hasArrived());
        assertEquals("IC713", route.getLegs()[0].getVehicleInformation().getId());
        assertEquals(route.getLegs()[0], route.getTransfers()[0].getDepartureLeg());
        assertEquals("Poperinge", route.getLegs()[0].getVehicleInformation().getHeadsign());
        assertEquals(new DateTime((long) 1510839180 * 1000), route.getLegs()[0].getArrival().getTime().withZone(DateTimeZone.UTC));
        assertEquals(new DateTime((long) 1510839180 * 1000), route.getTransfers()[1].getArrivalTime().withZone(DateTimeZone.UTC));

        assertEquals(true, route.getTransfers()[1].hasLeft());
        assertEquals("11", route.getTransfers()[1].getDeparturePlatform());
        assertNotNull( route.getTransfers()[1].getDepartureLeg());
        assertEquals("IC1513", route.getTransfers()[1].getDepartureLeg().getVehicleInformation().getId());
        assertEquals("IC1513", route.getLegs()[1].getVehicleInformation().getId());
        assertEquals("Genk", route.getTransfers()[1].getDepartureLeg().getVehicleInformation().getHeadsign());
        assertEquals(new DateTime((long) 1510839600 * 1000), route.getTransfers()[1].getDepartureTime().withZone(DateTimeZone.UTC));

        assertEquals("IC713", route.getLegs()[0].getVehicleInformation().getId());
        assertEquals("Poperinge", route.getLegs()[0].getVehicleInformation().getHeadsign());
        assertEquals("IC1513", route.getLegs()[1].getVehicleInformation().getId());
        assertEquals("Genk", route.getLegs()[1].getVehicleInformation().getHeadsign());
    }

    private static final String LIVEBOARD_RESPONSE = "{\n" +
            "\n" +
            "    \"version\": \"1.1\",\n" +
            "    \"timestamp\": \"1510836280\",\n" +
            "    \"station\": \"Ghent-Sint-Pieters\",\n" +
            "    \"stationinfo\": {\n" +
            "        \"id\": \"BE.NMBS.008892007\",\n" +
            "        \"locationX\": \"3.710675\",\n" +
            "        \"locationY\": \"51.035896\",\n" +
            "        \"@id\": \"http://irail.be/stations/NMBS/008892007\",\n" +
            "        \"name\": \"Ghent-Sint-Pieters\",\n" +
            "        \"standardname\": \"Gent-Sint-Pieters\"\n" +
            "    },\n" +
            "    \"departures\": {\n" +
            "        \"number\": \"31\",\n" +
            "        \"departure\": [\n" +
            "            {\n" +
            "                \"id\": \"0\",\n" +
            "                \"delay\": \"1860\",\n" +
            "                \"station\": \"De Panne\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008892338\",\n" +
            "                    \"locationX\": \"2.601963\",\n" +
            "                    \"locationY\": \"51.0774\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008892338\",\n" +
            "                    \"standardname\": \"De Panne\",\n" +
            "                    \"name\": \"De Panne\"\n" +
            "                },\n" +
            "                \"time\": \"1510833300\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC3634\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.IC3634\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/TESTPARSEURI\"\n" +
            "                },\n" +
            "                \"platform\": \"6\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"6\",\n" +
            "                    \"normal\": \"0\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC3634\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"1\",\n" +
            "                \"delay\": \"60\",\n" +
            "                \"station\": \"Ronse\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008892908\",\n" +
            "                    \"locationX\": \"3.602552\",\n" +
            "                    \"locationY\": \"50.742506\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008892908\",\n" +
            "                    \"standardname\": \"Ronse\",\n" +
            "                    \"name\": \"Ronse\"\n" +
            "                },\n" +
            "                \"time\": \"1510833600\",\n" +
            "                \"vehicle\": \"BE.NMBS.L783\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.L783\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/L783\"\n" +
            "                },\n" +
            "                \"platform\": \"4\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"4\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/DEPARTURECONNECTIONTEST\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"HIGH\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/HIGH\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"2\",\n" +
            "                \"delay\": \"0\",\n" +
            "                \"station\": \"Brugge\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008891009\",\n" +
            "                    \"locationX\": \"3.216726\",\n" +
            "                    \"locationY\": \"51.197226\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008891009\",\n" +
            "                    \"standardname\": \"Brugge\",\n" +
            "                    \"name\": \"Brugge\"\n" +
            "                },\n" +
            "                \"time\": \"1510833600\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC2833\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.IC2833\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/IC2833\"\n" +
            "                },\n" +
            "                \"platform\": \"12\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"12\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC2833\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"3\",\n" +
            "                \"delay\": \"60\",\n" +
            "                \"station\": \"Kortrijk\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008896008\",\n" +
            "                    \"locationX\": \"3.264549\",\n" +
            "                    \"locationY\": \"50.824506\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008896008\",\n" +
            "                    \"standardname\": \"Kortrijk\",\n" +
            "                    \"name\": \"Kortrijk\"\n" +
            "                },\n" +
            "                \"time\": \"1510833600\",\n" +
            "                \"vehicle\": \"BE.NMBS.L783\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.L783\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/L783\"\n" +
            "                },\n" +
            "                \"platform\": \"4\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"4\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/L783\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"4\",\n" +
            "                \"delay\": \"60\",\n" +
            "                \"station\": \"Leuven\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008833001\",\n" +
            "                    \"locationX\": \"4.715866\",\n" +
            "                    \"locationY\": \"50.88228\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008833001\",\n" +
            "                    \"standardname\": \"Leuven\",\n" +
            "                    \"name\": \"Leuven\"\n" +
            "                },\n" +
            "                \"time\": \"1510833660\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC4113\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.IC4113\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/IC4113\"\n" +
            "                },\n" +
            "                \"platform\": \"3\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"3\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC4113\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"5\",\n" +
            "                \"delay\": \"0\",\n" +
            "                \"station\": \"Geraardsbergen\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008895505\",\n" +
            "                    \"locationX\": \"3.872328\",\n" +
            "                    \"locationY\": \"50.771137\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008895505\",\n" +
            "                    \"standardname\": \"Geraardsbergen\",\n" +
            "                    \"name\": \"Geraardsbergen\"\n" +
            "                },\n" +
            "                \"time\": \"1510833900\",\n" +
            "                \"vehicle\": \"BE.NMBS.L1863\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.L1863\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/L1863\"\n" +
            "                },\n" +
            "                \"platform\": \"5\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"5\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/L1863\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"6\",\n" +
            "                \"delay\": \"60\",\n" +
            "                \"station\": \"Antwerp-Central\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008821006\",\n" +
            "                    \"locationX\": \"4.421101\",\n" +
            "                    \"locationY\": \"51.2172\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008821006\",\n" +
            "                    \"name\": \"Antwerp-Central\",\n" +
            "                    \"standardname\": \"Antwerpen-Centraal\"\n" +
            "                },\n" +
            "                \"time\": \"1510834020\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC3034\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.IC3034\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/IC3034\"\n" +
            "                },\n" +
            "                \"platform\": \"2\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"2\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC3034\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"7\",\n" +
            "                \"delay\": \"840\",\n" +
            "                \"station\": \"Kortrijk\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008896008\",\n" +
            "                    \"locationX\": \"3.264549\",\n" +
            "                    \"locationY\": \"50.824506\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008896008\",\n" +
            "                    \"standardname\": \"Kortrijk\",\n" +
            "                    \"name\": \"Kortrijk\"\n" +
            "                },\n" +
            "                \"time\": \"1510834140\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC433\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.IC433\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/IC433\"\n" +
            "                },\n" +
            "                \"platform\": \"7\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"7\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC433\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"8\",\n" +
            "                \"delay\": \"240\",\n" +
            "                \"station\": \"Brussels Airport - Zaventem\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008819406\",\n" +
            "                    \"locationX\": \"4.482076\",\n" +
            "                    \"locationY\": \"50.896456\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008819406\",\n" +
            "                    \"standardname\": \"Brussels Airport - Zaventem\",\n" +
            "                    \"name\": \"Brussels Airport - Zaventem\"\n" +
            "                },\n" +
            "                \"time\": \"1510834140\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC2812\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.IC2812\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/IC2812\"\n" +
            "                },\n" +
            "                \"platform\": \"11\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"11\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC2812\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"9\",\n" +
            "                \"delay\": \"240\",\n" +
            "                \"station\": \"Oostende\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008891702\",\n" +
            "                    \"locationX\": \"2.925809\",\n" +
            "                    \"locationY\": \"51.228212\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008891702\",\n" +
            "                    \"standardname\": \"Oostende\",\n" +
            "                    \"name\": \"Oostende\"\n" +
            "                },\n" +
            "                \"time\": \"1510834200\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC1833\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.IC1833\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/IC1833\"\n" +
            "                },\n" +
            "                \"platform\": \"10\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"10\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC1833\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"10\",\n" +
            "                \"delay\": \"0\",\n" +
            "                \"station\": \"Eeklo\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008893708\",\n" +
            "                    \"locationX\": \"3.574515\",\n" +
            "                    \"locationY\": \"51.181333\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008893708\",\n" +
            "                    \"standardname\": \"Eeklo\",\n" +
            "                    \"name\": \"Eeklo\"\n" +
            "                },\n" +
            "                \"time\": \"1510834320\",\n" +
            "                \"vehicle\": \"BE.NMBS.L762\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.L762\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/L762\"\n" +
            "                },\n" +
            "                \"platform\": \"1\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"1\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/L762\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"11\",\n" +
            "                \"delay\": \"180\",\n" +
            "                \"station\": \"Landen\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008833605\",\n" +
            "                    \"locationX\": \"5.07966\",\n" +
            "                    \"locationY\": \"50.747927\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008833605\",\n" +
            "                    \"standardname\": \"Landen\",\n" +
            "                    \"name\": \"Landen\"\n" +
            "                },\n" +
            "                \"time\": \"1510834380\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC3611\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.IC3611\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/IC3611\"\n" +
            "                },\n" +
            "                \"platform\": \"3\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"3\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC3611\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"12\",\n" +
            "                \"delay\": \"60\",\n" +
            "                \"station\": \"Mechelen\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008822004\",\n" +
            "                    \"locationX\": \"4.482785\",\n" +
            "                    \"locationY\": \"51.017648\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008822004\",\n" +
            "                    \"standardname\": \"Mechelen\",\n" +
            "                    \"name\": \"Mechelen\"\n" +
            "                },\n" +
            "                \"time\": \"1510834800\",\n" +
            "                \"vehicle\": \"BE.NMBS.L562\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.L562\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/L562\"\n" +
            "                },\n" +
            "                \"platform\": \"2\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"2\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/L562\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"13\",\n" +
            "                \"delay\": \"0\",\n" +
            "                \"station\": \"Eupen\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008844628\",\n" +
            "                    \"locationX\": \"6.03711\",\n" +
            "                    \"locationY\": \"50.635157\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008844628\",\n" +
            "                    \"standardname\": \"Eupen\",\n" +
            "                    \"name\": \"Eupen\"\n" +
            "                },\n" +
            "                \"time\": \"1510835040\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC512\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.IC512\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/IC512\"\n" +
            "                },\n" +
            "                \"platform\": \"11\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"11\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC512\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"14\",\n" +
            "                \"delay\": \"60\",\n" +
            "                \"station\": \"Blankenberge\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008891405\",\n" +
            "                    \"locationX\": \"3.133864\",\n" +
            "                    \"locationY\": \"51.312432\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008891405\",\n" +
            "                    \"standardname\": \"Blankenberge\",\n" +
            "                    \"name\": \"Blankenberge\"\n" +
            "                },\n" +
            "                \"time\": \"1510835040\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC1534\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.IC1534\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/IC1534\"\n" +
            "                },\n" +
            "                \"platform\": \"12\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"12\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC1534\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"15\",\n" +
            "                \"delay\": \"60\",\n" +
            "                \"station\": \"Knokke\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008891660\",\n" +
            "                    \"locationX\": \"3.285188\",\n" +
            "                    \"locationY\": \"51.339894\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008891660\",\n" +
            "                    \"standardname\": \"Knokke\",\n" +
            "                    \"name\": \"Knokke\"\n" +
            "                },\n" +
            "                \"time\": \"1510835040\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC1534\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.IC1534\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/IC1534\"\n" +
            "                },\n" +
            "                \"platform\": \"12\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"12\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC1534\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"16\",\n" +
            "                \"delay\": \"120\",\n" +
            "                \"station\": \"Antwerp-Central\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008821006\",\n" +
            "                    \"locationX\": \"4.421101\",\n" +
            "                    \"locationY\": \"51.2172\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008821006\",\n" +
            "                    \"name\": \"Antwerp-Central\",\n" +
            "                    \"standardname\": \"Antwerpen-Centraal\"\n" +
            "                },\n" +
            "                \"time\": \"1510835160\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC734\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.IC734\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/IC734\"\n" +
            "                },\n" +
            "                \"platform\": \"1\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"1\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC734\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"17\",\n" +
            "                \"delay\": \"120\",\n" +
            "                \"station\": \"Antwerp-Central\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008821006\",\n" +
            "                    \"locationX\": \"4.421101\",\n" +
            "                    \"locationY\": \"51.2172\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008821006\",\n" +
            "                    \"name\": \"Antwerp-Central\",\n" +
            "                    \"standardname\": \"Antwerpen-Centraal\"\n" +
            "                },\n" +
            "                \"time\": \"1510835160\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC734\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.IC734\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/IC734\"\n" +
            "                },\n" +
            "                \"platform\": \"1\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"1\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC734\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"18\",\n" +
            "                \"delay\": \"240\",\n" +
            "                \"station\": \"Poperinge\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008896735\",\n" +
            "                    \"locationX\": \"2.736343\",\n" +
            "                    \"locationY\": \"50.854449\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008896735\",\n" +
            "                    \"standardname\": \"Poperinge\",\n" +
            "                    \"name\": \"Poperinge\"\n" +
            "                },\n" +
            "                \"time\": \"1510835760\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC712\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.IC712\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/IC712\"\n" +
            "                },\n" +
            "                \"platform\": \"2\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"2\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC712\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"19\",\n" +
            "                \"delay\": \"240\",\n" +
            "                \"station\": \"Lille Flandres\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008728600\",\n" +
            "                    \"locationX\": \"3.066669\",\n" +
            "                    \"locationY\": \"50.633333\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008728600\",\n" +
            "                    \"standardname\": \"Lille Flandres\",\n" +
            "                    \"name\": \"Lille Flandres\"\n" +
            "                },\n" +
            "                \"time\": \"1510835760\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC712\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.IC712\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/IC712\"\n" +
            "                },\n" +
            "                \"platform\": \"2\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"2\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC712\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"20\",\n" +
            "                \"delay\": \"0\",\n" +
            "                \"station\": \"Oostende\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008891702\",\n" +
            "                    \"locationX\": \"2.925809\",\n" +
            "                    \"locationY\": \"51.228212\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008891702\",\n" +
            "                    \"standardname\": \"Oostende\",\n" +
            "                    \"name\": \"Oostende\"\n" +
            "                },\n" +
            "                \"time\": \"1510835940\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC534\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.IC534\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/IC534\"\n" +
            "                },\n" +
            "                \"platform\": \"12\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"12\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC534\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"21\",\n" +
            "                \"delay\": \"0\",\n" +
            "                \"station\": \"Genk\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008831765\",\n" +
            "                    \"locationX\": \"5.497685\",\n" +
            "                    \"locationY\": \"50.967057\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008831765\",\n" +
            "                    \"standardname\": \"Genk\",\n" +
            "                    \"name\": \"Genk\"\n" +
            "                },\n" +
            "                \"time\": \"1510836000\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC1512\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.IC1512\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/IC1512\"\n" +
            "                },\n" +
            "                \"platform\": \"11\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"11\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC1512\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"22\",\n" +
            "                \"delay\": \"0\",\n" +
            "                \"station\": \"Tongeren\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008831310\",\n" +
            "                    \"locationX\": \"5.47328\",\n" +
            "                    \"locationY\": \"50.784405\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008831310\",\n" +
            "                    \"standardname\": \"Tongeren\",\n" +
            "                    \"name\": \"Tongeren\"\n" +
            "                },\n" +
            "                \"time\": \"1510836000\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC2213\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.IC2213\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/IC2213\"\n" +
            "                },\n" +
            "                \"platform\": \"6\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"6\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC2213\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"23\",\n" +
            "                \"delay\": \"0\",\n" +
            "                \"station\": \"Zeebrugge-Dorp\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008891553\",\n" +
            "                    \"locationX\": \"3.19517\",\n" +
            "                    \"locationY\": \"51.326383\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008891553\",\n" +
            "                    \"standardname\": \"Zeebrugge-Dorp\",\n" +
            "                    \"name\": \"Zeebrugge-Dorp\"\n" +
            "                },\n" +
            "                \"time\": \"1510836300\",\n" +
            "                \"vehicle\": \"BE.NMBS.L584\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.L584\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/L584\"\n" +
            "                },\n" +
            "                \"platform\": \"10\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"10\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/L584\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"24\",\n" +
            "                \"delay\": \"0\",\n" +
            "                \"station\": \"Antwerp-Central\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008821006\",\n" +
            "                    \"locationX\": \"4.421101\",\n" +
            "                    \"locationY\": \"51.2172\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008821006\",\n" +
            "                    \"name\": \"Antwerp-Central\",\n" +
            "                    \"standardname\": \"Antwerpen-Centraal\"\n" +
            "                },\n" +
            "                \"time\": \"1510836780\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC1813\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.IC1813\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/IC1813\"\n" +
            "                },\n" +
            "                \"platform\": \"2\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"2\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC1813\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"25\",\n" +
            "                \"delay\": \"0\",\n" +
            "                \"station\": \"Welkenraedt\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008844503\",\n" +
            "                    \"locationX\": \"5.975381\",\n" +
            "                    \"locationY\": \"50.659707\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008844503\",\n" +
            "                    \"standardname\": \"Welkenraedt\",\n" +
            "                    \"name\": \"Welkenraedt\"\n" +
            "                },\n" +
            "                \"time\": \"1510836840\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC413\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.IC413\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/IC413\"\n" +
            "                },\n" +
            "                \"platform\": \"6\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"6\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC413\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"26\",\n" +
            "                \"delay\": \"0\",\n" +
            "                \"station\": \"De Panne\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008892338\",\n" +
            "                    \"locationX\": \"2.601963\",\n" +
            "                    \"locationY\": \"51.0774\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008892338\",\n" +
            "                    \"standardname\": \"De Panne\",\n" +
            "                    \"name\": \"De Panne\"\n" +
            "                },\n" +
            "                \"time\": \"1510836900\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC3635\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.IC3635\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/IC3635\"\n" +
            "                },\n" +
            "                \"platform\": \"7\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"7\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC3635\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"27\",\n" +
            "                \"delay\": \"0\",\n" +
            "                \"station\": \"Ronse\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008892908\",\n" +
            "                    \"locationX\": \"3.602552\",\n" +
            "                    \"locationY\": \"50.742506\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008892908\",\n" +
            "                    \"standardname\": \"Ronse\",\n" +
            "                    \"name\": \"Ronse\"\n" +
            "                },\n" +
            "                \"time\": \"1510837200\",\n" +
            "                \"vehicle\": \"BE.NMBS.L784\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.L784\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/L784\"\n" +
            "                },\n" +
            "                \"platform\": \"5\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"5\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/L784\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"28\",\n" +
            "                \"delay\": \"0\",\n" +
            "                \"station\": \"Brugge\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008891009\",\n" +
            "                    \"locationX\": \"3.216726\",\n" +
            "                    \"locationY\": \"51.197226\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008891009\",\n" +
            "                    \"standardname\": \"Brugge\",\n" +
            "                    \"name\": \"Brugge\"\n" +
            "                },\n" +
            "                \"time\": \"1510837200\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC2834\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.IC2834\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/IC2834\"\n" +
            "                },\n" +
            "                \"platform\": \"12\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"12\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC2834\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"29\",\n" +
            "                \"delay\": \"0\",\n" +
            "                \"station\": \"Kortrijk\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008896008\",\n" +
            "                    \"locationX\": \"3.264549\",\n" +
            "                    \"locationY\": \"50.824506\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008896008\",\n" +
            "                    \"standardname\": \"Kortrijk\",\n" +
            "                    \"name\": \"Kortrijk\"\n" +
            "                },\n" +
            "                \"time\": \"1510837200\",\n" +
            "                \"vehicle\": \"BE.NMBS.L784\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.L784\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/L784\"\n" +
            "                },\n" +
            "                \"platform\": \"5\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"5\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/L784\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"30\",\n" +
            "                \"delay\": \"0\",\n" +
            "                \"station\": \"Leuven\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008833001\",\n" +
            "                    \"locationX\": \"4.715866\",\n" +
            "                    \"locationY\": \"50.88228\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008833001\",\n" +
            "                    \"standardname\": \"Leuven\",\n" +
            "                    \"name\": \"Leuven\"\n" +
            "                },\n" +
            "                \"time\": \"1510837260\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC4114\",\n" +
            "                \"vehicleinfo\": {\n" +
            "                    \"name\": \"BE.NMBS.IC4114\",\n" +
            "                    \"@id\": \"http://irail.be/vehicle/IC4114\"\n" +
            "                },\n" +
            "                \"platform\": \"1\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"1\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC4114\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"name\": \"unknown\",\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\"\n" +
            "                }\n" +
            "            }\n" +
            "        ]\n" +
            "    }\n" +
            "\n" +
            "}";

    private static final String TRAIN_RESPONSE = "{\n" +
            "\n" +
            "    \"version\": \"1.1\",\n" +
            "    \"timestamp\": \"1510838092\",\n" +
            "    \"vehicle\": \"BE.NMBS.IC537\",\n" +
            "    \"vehicleinfo\": {\n" +
            "        \"locationX\": \"5.566695\"," +
            "        \"locationY\": \"50.62455\",\n" +
            "        \"name\": \"BE.NMBS.IC537\",\n" +
            "        \"shortname\": \"IC537\",\n" +
            "        \"@id\": \"http://irail.be/vehicle/PARSETHISVALUE\"\n" +
            "    },\n" +
            "    \"stops\": {\n" +
            "        \"number\": \"11\",\n" +
            "        \"stop\": [\n" +
            "            {\n" +
            "                \"id\": \"0\",\n" +
            "                \"station\": \"Eupen\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008844628\",\n" +
            "                    \"locationX\": \"6.03711\",\n" +
            "                    \"locationY\": \"50.635157\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008844628\",\n" +
            "                    \"standardname\": \"Eupen\",\n" +
            "                    \"name\": \"Eupen\"\n" +
            "                },\n" +
            "                \"time\": \"1510838220\",\n" +
            "                \"delay\": \"0\",\n" +
            "                \"platform\": \"2\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"2\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"departureDelay\": \"0\",\n" +
            "                \"departureCanceled\": \"0\",\n" +
            "                \"scheduledDepartureTime\": \"1510838220\",\n" +
            "                \"scheduledArrivalTime\": \"1510838220\",\n" +
            "                \"arrivalDelay\": \"0\",\n" +
            "                \"arrivalCanceled\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8844628/20171116/IC537\",\n" +
            "                \"left\": \"1\",\n" +
            "                \"isExtraStop\": \"0\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                    \"name\": \"unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"1\",\n" +
            "                \"station\": \"Welkenraedt\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008844503\",\n" +
            "                    \"locationX\": \"5.975381\",\n" +
            "                    \"locationY\": \"50.659707\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008844503\",\n" +
            "                    \"standardname\": \"Welkenraedt\",\n" +
            "                    \"name\": \"Welkenraedt\"\n" +
            "                },\n" +
            "                \"time\": \"1510838760\",\n" +
            "                \"delay\": \"0\",\n" +
            "                \"platform\": \"5\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"5\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"departureDelay\": \"0\",\n" +
            "                \"departureCanceled\": \"0\",\n" +
            "                \"scheduledDepartureTime\": \"1510838760\",\n" +
            "                \"scheduledArrivalTime\": \"1510838640\",\n" +
            "                \"arrivalDelay\": \"0\",\n" +
            "                \"arrivalCanceled\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8844503/20171116/IC537\",\n" +
            "                \"left\": \"1\",\n" +
            "                \"isExtraStop\": \"0\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                    \"name\": \"unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"2\",\n" +
            "                \"station\": \"Verviers-Centraal\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008844008\",\n" +
            "                    \"locationX\": \"5.854917\",\n" +
            "                    \"locationY\": \"50.588135\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008844008\",\n" +
            "                    \"name\": \"Verviers-Centraal\",\n" +
            "                    \"standardname\": \"Verviers-Central\"\n" +
            "                },\n" +
            "                \"time\": \"1510839540\",\n" +
            "                \"delay\": \"60\",\n" +
            "                \"platform\": \"1\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"1\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"departureDelay\": \"60\",\n" +
            "                \"departureCanceled\": \"0\",\n" +
            "                \"scheduledDepartureTime\": \"1510839540\",\n" +
            "                \"scheduledArrivalTime\": \"1510839480\",\n" +
            "                \"arrivalDelay\": \"120\",\n" +
            "                \"arrivalCanceled\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8844008/20171116/IC537\",\n" +
            "                \"left\": \"1\",\n" +
            "                \"isExtraStop\": \"0\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                    \"name\": \"unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"3\",\n" +
            "                \"station\": \"Luik-Guillemins\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008841004\",\n" +
            "                    \"locationX\": \"5.566695\",\n" +
            "                    \"locationY\": \"50.62455\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008841004\",\n" +
            "                    \"name\": \"Luik-Guillemins\",\n" +
            "                    \"standardname\": \"Liège-Guillemins\"\n" +
            "                },\n" +
            "                \"time\": \"1510840860\",\n" +
            "                \"delay\": \"0\",\n" +
            "                \"platform\": \"3\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"3\",\n" +
            "                    \"normal\": \"0\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"departureDelay\": \"0\",\n" +
            "                \"departureCanceled\": \"0\",\n" +
            "                \"scheduledDepartureTime\": \"1510840860\",\n" +
            "                \"scheduledArrivalTime\": \"1510840560\",\n" +
            "                \"arrivalDelay\": \"0\",\n" +
            "                \"arrivalCanceled\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8841004/20171116/IC537\",\n" +
            "                \"left\": \"1\",\n" +
            "                \"isExtraStop\": \"0\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                    \"name\": \"unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"4\",\n" +
            "                \"station\": \"Leuven\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008833001\",\n" +
            "                    \"locationX\": \"4.715866\",\n" +
            "                    \"locationY\": \"50.88228\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008833001\",\n" +
            "                    \"standardname\": \"Leuven\",\n" +
            "                    \"name\": \"Leuven\"\n" +
            "                },\n" +
            "                \"time\": \"1510842840\",\n" +
            "                \"delay\": \"0\",\n" +
            "                \"platform\": \"2\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"2\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"departureDelay\": \"0\",\n" +
            "                \"departureCanceled\": \"0\",\n" +
            "                \"scheduledDepartureTime\": \"1510842840\",\n" +
            "                \"scheduledArrivalTime\": \"1510842660\",\n" +
            "                \"arrivalDelay\": \"0\",\n" +
            "                \"arrivalCanceled\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8833001/20171116/IC537\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"isExtraStop\": \"0\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                    \"name\": \"unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"5\",\n" +
            "                \"station\": \"Brussel-Noord\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008812005\",\n" +
            "                    \"locationX\": \"4.360846\",\n" +
            "                    \"locationY\": \"50.859663\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008812005\",\n" +
            "                    \"name\": \"Brussel-Noord\",\n" +
            "                    \"standardname\": \"Brussel-Noord/Bruxelles-Nord\"\n" +
            "                },\n" +
            "                \"time\": \"1510843920\",\n" +
            "                \"delay\": \"0\",\n" +
            "                \"platform\": \"4\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"4\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"departureDelay\": \"0\",\n" +
            "                \"departureCanceled\": \"0\",\n" +
            "                \"scheduledDepartureTime\": \"1510843920\",\n" +
            "                \"scheduledArrivalTime\": \"1510843800\",\n" +
            "                \"arrivalDelay\": \"0\",\n" +
            "                \"arrivalCanceled\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8812005/20171116/IC537\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"isExtraStop\": \"0\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                    \"name\": \"unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"6\",\n" +
            "                \"station\": \"Brussel-Centraal\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008813003\",\n" +
            "                    \"locationX\": \"4.356801\",\n" +
            "                    \"locationY\": \"50.845658\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008813003\",\n" +
            "                    \"name\": \"Brussel-Centraal\",\n" +
            "                    \"standardname\": \"Brussel-Centraal/Bruxelles-Central\"\n" +
            "                },\n" +
            "                \"time\": \"1510844160\",\n" +
            "                \"delay\": \"0\",\n" +
            "                \"platform\": \"2\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"2\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"departureDelay\": \"0\",\n" +
            "                \"departureCanceled\": \"0\",\n" +
            "                \"scheduledDepartureTime\": \"1510844160\",\n" +
            "                \"scheduledArrivalTime\": \"1510844100\",\n" +
            "                \"arrivalDelay\": \"0\",\n" +
            "                \"arrivalCanceled\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8813003/20171116/IC537\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"isExtraStop\": \"0\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                    \"name\": \"unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"7\",\n" +
            "                \"station\": \"Brussel-Zuid\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008814001\",\n" +
            "                    \"locationX\": \"4.336531\",\n" +
            "                    \"locationY\": \"50.835707\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008814001\",\n" +
            "                    \"name\": \"Brussel-Zuid\",\n" +
            "                    \"standardname\": \"Brussel-Zuid/Bruxelles-Midi\"\n" +
            "                },\n" +
            "                \"time\": \"1510844640\",\n" +
            "                \"delay\": \"0\",\n" +
            "                \"platform\": \"11\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"11\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"departureDelay\": \"0\",\n" +
            "                \"departureCanceled\": \"0\",\n" +
            "                \"scheduledDepartureTime\": \"1510844640\",\n" +
            "                \"scheduledArrivalTime\": \"1510844400\",\n" +
            "                \"arrivalDelay\": \"0\",\n" +
            "                \"arrivalCanceled\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8814001/20171116/IC537\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"isExtraStop\": \"0\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                    \"name\": \"unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"8\",\n" +
            "                \"station\": \"Gent-Sint-Pieters\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008892007\",\n" +
            "                    \"locationX\": \"3.710675\",\n" +
            "                    \"locationY\": \"51.035896\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008892007\",\n" +
            "                    \"standardname\": \"Gent-Sint-Pieters\",\n" +
            "                    \"name\": \"Gent-Sint-Pieters\"\n" +
            "                },\n" +
            "                \"time\": \"1510846740\",\n" +
            "                \"delay\": \"0\",\n" +
            "                \"platform\": \"12\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"12\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"departureDelay\": \"0\",\n" +
            "                \"departureCanceled\": \"0\",\n" +
            "                \"scheduledDepartureTime\": \"1510846740\",\n" +
            "                \"scheduledArrivalTime\": \"1510846500\",\n" +
            "                \"arrivalDelay\": \"0\",\n" +
            "                \"arrivalCanceled\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC537\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"isExtraStop\": \"0\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                    \"name\": \"unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"9\",\n" +
            "                \"station\": \"Brugge\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008891009\",\n" +
            "                    \"locationX\": \"3.216726\",\n" +
            "                    \"locationY\": \"51.197226\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008891009\",\n" +
            "                    \"standardname\": \"Brugge\",\n" +
            "                    \"name\": \"Brugge\"\n" +
            "                },\n" +
            "                \"time\": \"1510848240\",\n" +
            "                \"delay\": \"0\",\n" +
            "                \"platform\": \"10\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"10\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"departureDelay\": \"0\",\n" +
            "                \"departureCanceled\": \"0\",\n" +
            "                \"scheduledDepartureTime\": \"1510848240\",\n" +
            "                \"scheduledArrivalTime\": \"1510848120\",\n" +
            "                \"arrivalDelay\": \"0\",\n" +
            "                \"arrivalCanceled\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8891009/20171116/IC537\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"isExtraStop\": \"0\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                    \"name\": \"unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            {\n" +
            "                \"id\": \"10\",\n" +
            "                \"station\": \"Oostende\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008891702\",\n" +
            "                    \"locationX\": \"2.925809\",\n" +
            "                    \"locationY\": \"51.228212\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008891702\",\n" +
            "                    \"standardname\": \"Oostende\",\n" +
            "                    \"name\": \"Oostende\"\n" +
            "                },\n" +
            "                \"time\": \"1510849140\",\n" +
            "                \"delay\": \"0\",\n" +
            "                \"platform\": \"5\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"5\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"departureDelay\": \"0\",\n" +
            "                \"departureCanceled\": \"0\",\n" +
            "                \"scheduledDepartureTime\": \"1510849140\",\n" +
            "                \"scheduledArrivalTime\": \"1510849140\",\n" +
            "                \"arrivalDelay\": \"0\",\n" +
            "                \"arrivalCanceled\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8891702/20171116/IC537\",\n" +
            "                \"left\": \"0\",\n" +
            "                \"isExtraStop\": \"0\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                    \"name\": \"unknown\"\n" +
            "                }\n" +
            "            }\n" +
            "        ]\n" +
            "    }\n" +
            "\n" +
            "}";

    private static final String ROUTE_RESPONSE = "{\n" +
            "\n" +
            "    \"version\": \"1.1\",\n" +
            "    \"timestamp\": \"1510839771\",\n" +
            "    \"connection\": [\n" +
            "        {\n" +
            "            \"id\": \"0\",\n" +
            "            \"departure\": {\n" +
            "                \"delay\": \"120\",\n" +
            "                \"station\": \"Ghent-Dampoort\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008893120\",\n" +
            "                    \"locationX\": \"3.740591\",\n" +
            "                    \"locationY\": \"51.056365\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008893120\",\n" +
            "                    \"name\": \"Ghent-Dampoort\",\n" +
            "                    \"standardname\": \"Gent-Dampoort\"\n" +
            "                },\n" +
            "                \"time\": \"1510838640\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC713\",\n" +
            "                \"platform\": \"1\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"1\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8893120/20171116/IC713\",\n" +
            "                \"direction\": {\n" +
            "                    \"name\": \"Poperinge\"\n" +
            "                },\n" +
            "                \"left\": \"1\",\n" +
            "                \"walking\": \"0\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                    \"name\": \"unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            \"arrival\": {\n" +
            "                \"delay\": \"0\",\n" +
            "                \"station\": \"Kiewit\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008832375\",\n" +
            "                    \"locationX\": \"5.350226\",\n" +
            "                    \"locationY\": \"50.954841\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008832375\",\n" +
            "                    \"standardname\": \"Kiewit\",\n" +
            "                    \"name\": \"Kiewit\"\n" +
            "                },\n" +
            "                \"time\": \"1510846980\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC1513\",\n" +
            "                \"platform\": \"2\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"2\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"direction\": {\n" +
            "                    \"name\": \"Genk\"\n" +
            "                },\n" +
            "                \"arrived\": \"0\",\n" +
            "                \"walking\": \"0\"\n" +
            "            },\n" +
            "            \"duration\": \"8340\",\n" +
            "            \"alerts\": {\n" +
            "                \"number\": \"1\",\n" +
            "                \"alert\": [\n" +
            "                    {\n" +
            "                        \"id\": \"0\",\n" +
            "                        \"header\": \"Probleem bovenleiding  MIVB\",\n" +
            "                        \"description\": \"Reizigers tussen Brussel-Zuid en Brussel-Noord mogen met hun MIVB-ticket gebruik maken van de treinen van NMBS.\",\n" +
            "                        \"lead\": \"Probleem bovenleiding  MIVB\",\n" +
            "                        \"startTime\": \"1510834800\",\n" +
            "                        \"endTime\": \"1510873140\"\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            \"vias\": {\n" +
            "                \"number\": \"1\",\n" +
            "                \"via\": [\n" +
            "                    {\n" +
            "                        \"id\": \"0\",\n" +
            "                        \"arrival\": {\n" +
            "                            \"time\": \"1510839180\",\n" +
            "                            \"platform\": \"4\",\n" +
            "                            \"platforminfo\": {\n" +
            "                                \"name\": \"4\",\n" +
            "                                \"normal\": \"1\"\n" +
            "                            },\n" +
            "                            \"isExtraStop\": \"0\",\n" +
            "                            \"delay\": \"120\",\n" +
            "                            \"canceled\": \"0\",\n" +
            "                            \"arrived\": \"1\",\n" +
            "                            \"walking\": \"0\",\n" +
            "                            \"direction\": {\n" +
            "                                \"name\": \"Poperinge\"\n" +
            "                            },\n" +
            "                            \"vehicle\": \"BE.NMBS.IC713\",\n" +
            "                            \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC713\"\n" +
            "                        },\n" +
            "                        \"departure\": {\n" +
            "                            \"time\": \"1510839600\",\n" +
            "                            \"platform\": \"11\",\n" +
            "                            \"platforminfo\": {\n" +
            "                                \"name\": \"11\",\n" +
            "                                \"normal\": \"1\"\n" +
            "                            },\n" +
            "                            \"isExtraStop\": \"0\",\n" +
            "                            \"delay\": \"0\",\n" +
            "                            \"canceled\": \"0\",\n" +
            "                            \"alerts\": {\n" +
            "                                \"number\": \"1\",\n" +
            "                                \"alert\": [\n" +
            "                                    {\n" +
            "                                        \"id\": \"0\",\n" +
            "                                        \"header\": \"Probleem bovenleiding  MIVB\",\n" +
            "                                        \"description\": \"Reizigers tussen Brussel-Zuid en Brussel-Noord mogen met hun MIVB-ticket gebruik maken van de treinen van NMBS.\",\n" +
            "                                        \"lead\": \"Probleem bovenleiding  MIVB\",\n" +
            "                                        \"startTime\": \"1510834800\",\n" +
            "                                        \"endTime\": \"1510873140\"\n" +
            "                                    }\n" +
            "                                ]\n" +
            "                            },\n" +
            "                            \"left\": \"1\",\n" +
            "                            \"walking\": \"0\",\n" +
            "                            \"direction\": {\n" +
            "                                \"name\": \"Genk\"\n" +
            "                            },\n" +
            "                            \"vehicle\": \"BE.NMBS.IC1513\",\n" +
            "                            \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC1513\",\n" +
            "                            \"occupancy\": {\n" +
            "                                \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                                \"name\": \"unknown\"\n" +
            "                            }\n" +
            "                        },\n" +
            "                        \"timeBetween\": \"420\",\n" +
            "                        \"station\": \"Ghent-Sint-Pieters\",\n" +
            "                        \"stationinfo\": {\n" +
            "                            \"id\": \"BE.NMBS.008892007\",\n" +
            "                            \"locationX\": \"3.710675\",\n" +
            "                            \"locationY\": \"51.035896\",\n" +
            "                            \"@id\": \"http://irail.be/stations/NMBS/008892007\",\n" +
            "                            \"name\": \"Ghent-Sint-Pieters\",\n" +
            "                            \"standardname\": \"Gent-Sint-Pieters\"\n" +
            "                        },\n" +
            "                        \"vehicle\": \"BE.NMBS.IC713\",\n" +
            "                        \"direction\": {\n" +
            "                            \"name\": \"Poperinge\"\n" +
            "                        }\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            \"occupancy\": {\n" +
            "                \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                \"name\": \"unknown\"\n" +
            "            }\n" +
            "        },\n" +
            "        {\n" +
            "            \"id\": \"1\",\n" +
            "            \"departure\": {\n" +
            "                \"delay\": \"0\",\n" +
            "                \"station\": \"Ghent-Dampoort\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008893120\",\n" +
            "                    \"locationX\": \"3.740591\",\n" +
            "                    \"locationY\": \"51.056365\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008893120\",\n" +
            "                    \"name\": \"Ghent-Dampoort\",\n" +
            "                    \"standardname\": \"Gent-Dampoort\"\n" +
            "                },\n" +
            "                \"time\": \"1510840680\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC1835\",\n" +
            "                \"platform\": \"1\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"1\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8893120/20171116/IC1835\",\n" +
            "                \"direction\": {\n" +
            "                    \"name\": \"Oostende\"\n" +
            "                },\n" +
            "                \"left\": \"0\",\n" +
            "                \"walking\": \"0\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                    \"name\": \"unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            \"arrival\": {\n" +
            "                \"delay\": \"0\",\n" +
            "                \"station\": \"Kiewit\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008832375\",\n" +
            "                    \"locationX\": \"5.350226\",\n" +
            "                    \"locationY\": \"50.954841\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008832375\",\n" +
            "                    \"standardname\": \"Kiewit\",\n" +
            "                    \"name\": \"Kiewit\"\n" +
            "                },\n" +
            "                \"time\": \"1510849440\",\n" +
            "                \"vehicle\": \"BE.NMBS.P2367\",\n" +
            "                \"platform\": \"2\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"2\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"direction\": {\n" +
            "                    \"name\": \"Genk\"\n" +
            "                },\n" +
            "                \"arrived\": \"0\",\n" +
            "                \"walking\": \"0\"\n" +
            "            },\n" +
            "            \"duration\": \"8760\",\n" +
            "            \"alerts\": {\n" +
            "                \"number\": \"1\",\n" +
            "                \"alert\": [\n" +
            "                    {\n" +
            "                        \"id\": \"0\",\n" +
            "                        \"header\": \"Probleem bovenleiding  MIVB\",\n" +
            "                        \"description\": \"Reizigers tussen Brussel-Zuid en Brussel-Noord mogen met hun MIVB-ticket gebruik maken van de treinen van NMBS.\",\n" +
            "                        \"lead\": \"Probleem bovenleiding  MIVB\",\n" +
            "                        \"startTime\": \"1510834800\",\n" +
            "                        \"endTime\": \"1510873140\"\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            \"vias\": {\n" +
            "                \"number\": \"3\",\n" +
            "                \"via\": [\n" +
            "                    {\n" +
            "                        \"id\": \"0\",\n" +
            "                        \"arrival\": {\n" +
            "                            \"time\": \"1510841220\",\n" +
            "                            \"platform\": \"10\",\n" +
            "                            \"platforminfo\": {\n" +
            "                                \"name\": \"10\",\n" +
            "                                \"normal\": \"1\"\n" +
            "                            },\n" +
            "                            \"isExtraStop\": \"0\",\n" +
            "                            \"delay\": \"0\",\n" +
            "                            \"canceled\": \"0\",\n" +
            "                            \"arrived\": \"0\",\n" +
            "                            \"walking\": \"0\",\n" +
            "                            \"direction\": {\n" +
            "                                \"name\": \"Oostende\"\n" +
            "                            },\n" +
            "                            \"vehicle\": \"BE.NMBS.IC1835\",\n" +
            "                            \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC1835\"\n" +
            "                        },\n" +
            "                        \"departure\": {\n" +
            "                            \"time\": \"1510842240\",\n" +
            "                            \"platform\": \"11\",\n" +
            "                            \"platforminfo\": {\n" +
            "                                \"name\": \"11\",\n" +
            "                                \"normal\": \"1\"\n" +
            "                            },\n" +
            "                            \"isExtraStop\": \"0\",\n" +
            "                            \"delay\": \"0\",\n" +
            "                            \"canceled\": \"0\",\n" +
            "                            \"left\": \"0\",\n" +
            "                            \"walking\": \"0\",\n" +
            "                            \"direction\": {\n" +
            "                                \"name\": \"Eupen\"\n" +
            "                            },\n" +
            "                            \"vehicle\": \"BE.NMBS.IC514\",\n" +
            "                            \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC514\",\n" +
            "                            \"occupancy\": {\n" +
            "                                \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                                \"name\": \"unknown\"\n" +
            "                            }\n" +
            "                        },\n" +
            "                        \"timeBetween\": \"1020\",\n" +
            "                        \"station\": \"Ghent-Sint-Pieters\",\n" +
            "                        \"stationinfo\": {\n" +
            "                            \"id\": \"BE.NMBS.008892007\",\n" +
            "                            \"locationX\": \"3.710675\",\n" +
            "                            \"locationY\": \"51.035896\",\n" +
            "                            \"@id\": \"http://irail.be/stations/NMBS/008892007\",\n" +
            "                            \"name\": \"Ghent-Sint-Pieters\",\n" +
            "                            \"standardname\": \"Gent-Sint-Pieters\"\n" +
            "                        },\n" +
            "                        \"vehicle\": \"BE.NMBS.IC1835\",\n" +
            "                        \"direction\": {\n" +
            "                            \"name\": \"Oostende\"\n" +
            "                        }\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"id\": \"1\",\n" +
            "                        \"arrival\": {\n" +
            "                            \"time\": \"1510843980\",\n" +
            "                            \"platform\": \"14\",\n" +
            "                            \"platforminfo\": {\n" +
            "                                \"name\": \"14\",\n" +
            "                                \"normal\": \"1\"\n" +
            "                            },\n" +
            "                            \"isExtraStop\": \"0\",\n" +
            "                            \"delay\": \"0\",\n" +
            "                            \"canceled\": \"0\",\n" +
            "                            \"arrived\": \"0\",\n" +
            "                            \"walking\": \"0\",\n" +
            "                            \"direction\": {\n" +
            "                                \"name\": \"Eupen\"\n" +
            "                            },\n" +
            "                            \"vehicle\": \"BE.NMBS.IC514\",\n" +
            "                            \"departureConnection\": \"http://irail.be/connections/8814001/20171116/IC514\"\n" +
            "                        },\n" +
            "                        \"departure\": {\n" +
            "                            \"time\": \"1510844400\",\n" +
            "                            \"platform\": \"9\",\n" +
            "                            \"platforminfo\": {\n" +
            "                                \"name\": \"9\",\n" +
            "                                \"normal\": \"1\"\n" +
            "                            },\n" +
            "                            \"isExtraStop\": \"0\",\n" +
            "                            \"delay\": \"0\",\n" +
            "                            \"canceled\": \"0\",\n" +
            "                            \"alerts\": {\n" +
            "                                \"number\": \"1\",\n" +
            "                                \"alert\": [\n" +
            "                                    {\n" +
            "                                        \"id\": \"0\",\n" +
            "                                        \"header\": \"Probleem bovenleiding  MIVB\",\n" +
            "                                        \"description\": \"Reizigers tussen Brussel-Zuid en Brussel-Noord mogen met hun MIVB-ticket gebruik maken van de treinen van NMBS.\",\n" +
            "                                        \"lead\": \"Probleem bovenleiding  MIVB\",\n" +
            "                                        \"startTime\": \"1510834800\",\n" +
            "                                        \"endTime\": \"1510873140\"\n" +
            "                                    }\n" +
            "                                ]\n" +
            "                            },\n" +
            "                            \"left\": \"0\",\n" +
            "                            \"walking\": \"0\",\n" +
            "                            \"direction\": {\n" +
            "                                \"name\": \"Tongeren\"\n" +
            "                            },\n" +
            "                            \"vehicle\": \"BE.NMBS.P8305\",\n" +
            "                            \"departureConnection\": \"http://irail.be/connections/8814001/20171116/P8305\",\n" +
            "                            \"occupancy\": {\n" +
            "                                \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                                \"name\": \"unknown\"\n" +
            "                            }\n" +
            "                        },\n" +
            "                        \"timeBetween\": \"420\",\n" +
            "                        \"station\": \"Brussels-South/Brussels-Midi\",\n" +
            "                        \"stationinfo\": {\n" +
            "                            \"id\": \"BE.NMBS.008814001\",\n" +
            "                            \"locationX\": \"4.336531\",\n" +
            "                            \"locationY\": \"50.835707\",\n" +
            "                            \"@id\": \"http://irail.be/stations/NMBS/008814001\",\n" +
            "                            \"name\": \"Brussels-South/Brussels-Midi\",\n" +
            "                            \"standardname\": \"Brussel-Zuid/Bruxelles-Midi\"\n" +
            "                        },\n" +
            "                        \"vehicle\": \"BE.NMBS.IC514\",\n" +
            "                        \"direction\": {\n" +
            "                            \"name\": \"Eupen\"\n" +
            "                        }\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"id\": \"2\",\n" +
            "                        \"arrival\": {\n" +
            "                            \"time\": \"1510848420\",\n" +
            "                            \"platform\": \"3\",\n" +
            "                            \"platforminfo\": {\n" +
            "                                \"name\": \"3\",\n" +
            "                                \"normal\": \"1\"\n" +
            "                            },\n" +
            "                            \"isExtraStop\": \"0\",\n" +
            "                            \"delay\": \"0\",\n" +
            "                            \"canceled\": \"0\",\n" +
            "                            \"arrived\": \"0\",\n" +
            "                            \"walking\": \"0\",\n" +
            "                            \"direction\": {\n" +
            "                                \"name\": \"Tongeren\"\n" +
            "                            },\n" +
            "                            \"vehicle\": \"BE.NMBS.P8305\",\n" +
            "                            \"departureConnection\": \"http://irail.be/connections/8831005/20171116/P8305\"\n" +
            "                        },\n" +
            "                        \"departure\": {\n" +
            "                            \"time\": \"1510848960\",\n" +
            "                            \"platform\": \"7\",\n" +
            "                            \"platforminfo\": {\n" +
            "                                \"name\": \"7\",\n" +
            "                                \"normal\": \"1\"\n" +
            "                            },\n" +
            "                            \"isExtraStop\": \"0\",\n" +
            "                            \"delay\": \"0\",\n" +
            "                            \"canceled\": \"0\",\n" +
            "                            \"left\": \"0\",\n" +
            "                            \"walking\": \"0\",\n" +
            "                            \"direction\": {\n" +
            "                                \"name\": \"Genk\"\n" +
            "                            },\n" +
            "                            \"vehicle\": \"BE.NMBS.P2367\",\n" +
            "                            \"departureConnection\": \"http://irail.be/connections/8831005/20171116/P2367\",\n" +
            "                            \"occupancy\": {\n" +
            "                                \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                                \"name\": \"unknown\"\n" +
            "                            }\n" +
            "                        },\n" +
            "                        \"timeBetween\": \"540\",\n" +
            "                        \"station\": \"Hasselt\",\n" +
            "                        \"stationinfo\": {\n" +
            "                            \"id\": \"BE.NMBS.008831005\",\n" +
            "                            \"locationX\": \"5.327627\",\n" +
            "                            \"locationY\": \"50.930822\",\n" +
            "                            \"@id\": \"http://irail.be/stations/NMBS/008831005\",\n" +
            "                            \"standardname\": \"Hasselt\",\n" +
            "                            \"name\": \"Hasselt\"\n" +
            "                        },\n" +
            "                        \"vehicle\": \"BE.NMBS.P8305\",\n" +
            "                        \"direction\": {\n" +
            "                            \"name\": \"Tongeren\"\n" +
            "                        }\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            \"occupancy\": {\n" +
            "                \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                \"name\": \"unknown\"\n" +
            "            }\n" +
            "        },\n" +
            "        {\n" +
            "            \"id\": \"2\",\n" +
            "            \"departure\": {\n" +
            "                \"delay\": \"0\",\n" +
            "                \"station\": \"Ghent-Dampoort\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008893120\",\n" +
            "                    \"locationX\": \"3.740591\",\n" +
            "                    \"locationY\": \"51.056365\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008893120\",\n" +
            "                    \"name\": \"Ghent-Dampoort\",\n" +
            "                    \"standardname\": \"Gent-Dampoort\"\n" +
            "                },\n" +
            "                \"time\": \"1510842240\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC714\",\n" +
            "                \"platform\": \"1\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"1\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8893120/20171116/IC714\",\n" +
            "                \"direction\": {\n" +
            "                    \"name\": \"Poperinge\"\n" +
            "                },\n" +
            "                \"left\": \"0\",\n" +
            "                \"walking\": \"0\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                    \"name\": \"unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            \"arrival\": {\n" +
            "                \"delay\": \"0\",\n" +
            "                \"station\": \"Kiewit\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008832375\",\n" +
            "                    \"locationX\": \"5.350226\",\n" +
            "                    \"locationY\": \"50.954841\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008832375\",\n" +
            "                    \"standardname\": \"Kiewit\",\n" +
            "                    \"name\": \"Kiewit\"\n" +
            "                },\n" +
            "                \"time\": \"1510850580\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC1514\",\n" +
            "                \"platform\": \"2\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"2\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"direction\": {\n" +
            "                    \"name\": \"Genk\"\n" +
            "                },\n" +
            "                \"arrived\": \"0\",\n" +
            "                \"walking\": \"0\"\n" +
            "            },\n" +
            "            \"duration\": \"8340\",\n" +
            "            \"alerts\": {\n" +
            "                \"number\": \"1\",\n" +
            "                \"alert\": [\n" +
            "                    {\n" +
            "                        \"id\": \"0\",\n" +
            "                        \"header\": \"Probleem bovenleiding  MIVB\",\n" +
            "                        \"description\": \"Reizigers tussen Brussel-Zuid en Brussel-Noord mogen met hun MIVB-ticket gebruik maken van de treinen van NMBS.\",\n" +
            "                        \"lead\": \"Probleem bovenleiding  MIVB\",\n" +
            "                        \"startTime\": \"1510834800\",\n" +
            "                        \"endTime\": \"1510873140\"\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            \"vias\": {\n" +
            "                \"number\": \"1\",\n" +
            "                \"via\": [\n" +
            "                    {\n" +
            "                        \"id\": \"0\",\n" +
            "                        \"arrival\": {\n" +
            "                            \"time\": \"1510842780\",\n" +
            "                            \"platform\": \"2\",\n" +
            "                            \"platforminfo\": {\n" +
            "                                \"name\": \"2\",\n" +
            "                                \"normal\": \"1\"\n" +
            "                            },\n" +
            "                            \"isExtraStop\": \"0\",\n" +
            "                            \"delay\": \"0\",\n" +
            "                            \"canceled\": \"0\",\n" +
            "                            \"arrived\": \"0\",\n" +
            "                            \"walking\": \"0\",\n" +
            "                            \"direction\": {\n" +
            "                                \"name\": \"Poperinge\"\n" +
            "                            },\n" +
            "                            \"vehicle\": \"BE.NMBS.IC714\",\n" +
            "                            \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC714\"\n" +
            "                        },\n" +
            "                        \"departure\": {\n" +
            "                            \"time\": \"1510843200\",\n" +
            "                            \"platform\": \"11\",\n" +
            "                            \"platforminfo\": {\n" +
            "                                \"name\": \"11\",\n" +
            "                                \"normal\": \"1\"\n" +
            "                            },\n" +
            "                            \"isExtraStop\": \"0\",\n" +
            "                            \"delay\": \"0\",\n" +
            "                            \"canceled\": \"0\",\n" +
            "                            \"alerts\": {\n" +
            "                                \"number\": \"1\",\n" +
            "                                \"alert\": [\n" +
            "                                    {\n" +
            "                                        \"id\": \"0\",\n" +
            "                                        \"header\": \"Probleem bovenleiding  MIVB\",\n" +
            "                                        \"description\": \"Reizigers tussen Brussel-Zuid en Brussel-Noord mogen met hun MIVB-ticket gebruik maken van de treinen van NMBS.\",\n" +
            "                                        \"lead\": \"Probleem bovenleiding  MIVB\",\n" +
            "                                        \"startTime\": \"1510834800\",\n" +
            "                                        \"endTime\": \"1510873140\"\n" +
            "                                    }\n" +
            "                                ]\n" +
            "                            },\n" +
            "                            \"left\": \"0\",\n" +
            "                            \"walking\": \"0\",\n" +
            "                            \"direction\": {\n" +
            "                                \"name\": \"Genk\"\n" +
            "                            },\n" +
            "                            \"vehicle\": \"BE.NMBS.IC1514\",\n" +
            "                            \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC1514\",\n" +
            "                            \"occupancy\": {\n" +
            "                                \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                                \"name\": \"unknown\"\n" +
            "                            }\n" +
            "                        },\n" +
            "                        \"timeBetween\": \"420\",\n" +
            "                        \"station\": \"Ghent-Sint-Pieters\",\n" +
            "                        \"stationinfo\": {\n" +
            "                            \"id\": \"BE.NMBS.008892007\",\n" +
            "                            \"locationX\": \"3.710675\",\n" +
            "                            \"locationY\": \"51.035896\",\n" +
            "                            \"@id\": \"http://irail.be/stations/NMBS/008892007\",\n" +
            "                            \"name\": \"Ghent-Sint-Pieters\",\n" +
            "                            \"standardname\": \"Gent-Sint-Pieters\"\n" +
            "                        },\n" +
            "                        \"vehicle\": \"BE.NMBS.IC714\",\n" +
            "                        \"direction\": {\n" +
            "                            \"name\": \"Poperinge\"\n" +
            "                        }\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            \"occupancy\": {\n" +
            "                \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                \"name\": \"unknown\"\n" +
            "            }\n" +
            "        },\n" +
            "        {\n" +
            "            \"id\": \"3\",\n" +
            "            \"departure\": {\n" +
            "                \"delay\": \"0\",\n" +
            "                \"station\": \"Ghent-Dampoort\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008893120\",\n" +
            "                    \"locationX\": \"3.740591\",\n" +
            "                    \"locationY\": \"51.056365\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008893120\",\n" +
            "                    \"name\": \"Ghent-Dampoort\",\n" +
            "                    \"standardname\": \"Gent-Dampoort\"\n" +
            "                },\n" +
            "                \"time\": \"1510844580\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC1815\",\n" +
            "                \"platform\": \"2\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"2\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8893120/20171116/IC1815\",\n" +
            "                \"direction\": {\n" +
            "                    \"name\": \"Antwerpen-Centraal\"\n" +
            "                },\n" +
            "                \"left\": \"0\",\n" +
            "                \"walking\": \"0\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                    \"name\": \"unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            \"arrival\": {\n" +
            "                \"delay\": \"0\",\n" +
            "                \"station\": \"Kiewit\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008832375\",\n" +
            "                    \"locationX\": \"5.350226\",\n" +
            "                    \"locationY\": \"50.954841\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008832375\",\n" +
            "                    \"standardname\": \"Kiewit\",\n" +
            "                    \"name\": \"Kiewit\"\n" +
            "                },\n" +
            "                \"time\": \"1510853040\",\n" +
            "                \"vehicle\": \"BE.NMBS.P2368\",\n" +
            "                \"platform\": \"2\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"2\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"direction\": {\n" +
            "                    \"name\": \"Genk\"\n" +
            "                },\n" +
            "                \"arrived\": \"0\",\n" +
            "                \"walking\": \"0\"\n" +
            "            },\n" +
            "            \"duration\": \"8460\",\n" +
            "            \"remarks\": {\n" +
            "                \"number\": \"2\",\n" +
            "                \"remark\": [\n" +
            "                    {\n" +
            "                        \"id\": \"0\",\n" +
            "                        \"code\": \"WAITING\",\n" +
            "                        \"description\": \"The connection of P   8214 will be guaranteed as long as the delay is less than 6 min.\"\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"id\": \"1\",\n" +
            "                        \"code\": \"WAITING\",\n" +
            "                        \"description\": \"The connection of P   8214 will be guaranteed as long as the delay is less than 6 min.\"\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            \"vias\": {\n" +
            "                \"number\": \"2\",\n" +
            "                \"via\": [\n" +
            "                    {\n" +
            "                        \"id\": \"0\",\n" +
            "                        \"arrival\": {\n" +
            "                            \"time\": \"1510847280\",\n" +
            "                            \"platform\": \"7\",\n" +
            "                            \"platforminfo\": {\n" +
            "                                \"name\": \"7\",\n" +
            "                                \"normal\": \"1\"\n" +
            "                            },\n" +
            "                            \"isExtraStop\": \"0\",\n" +
            "                            \"delay\": \"0\",\n" +
            "                            \"canceled\": \"0\",\n" +
            "                            \"arrived\": \"0\",\n" +
            "                            \"walking\": \"0\",\n" +
            "                            \"direction\": {\n" +
            "                                \"name\": \"Antwerpen-Centraal\"\n" +
            "                            },\n" +
            "                            \"vehicle\": \"BE.NMBS.IC1815\",\n" +
            "                            \"departureConnection\": \"http://irail.be/connections/8821121/20171116/IC1815\"\n" +
            "                        },\n" +
            "                        \"departure\": {\n" +
            "                            \"time\": \"1510847820\",\n" +
            "                            \"platform\": \"5\",\n" +
            "                            \"platforminfo\": {\n" +
            "                                \"name\": \"5\",\n" +
            "                                \"normal\": \"1\"\n" +
            "                            },\n" +
            "                            \"isExtraStop\": \"0\",\n" +
            "                            \"delay\": \"0\",\n" +
            "                            \"canceled\": \"0\",\n" +
            "                            \"left\": \"0\",\n" +
            "                            \"walking\": \"0\",\n" +
            "                            \"direction\": {\n" +
            "                                \"name\": \"Hasselt\"\n" +
            "                            },\n" +
            "                            \"vehicle\": \"BE.NMBS.P8214\",\n" +
            "                            \"departureConnection\": \"http://irail.be/connections/8821121/20171116/P8214\",\n" +
            "                            \"occupancy\": {\n" +
            "                                \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                                \"name\": \"unknown\"\n" +
            "                            }\n" +
            "                        },\n" +
            "                        \"timeBetween\": \"540\",\n" +
            "                        \"station\": \"Antwerp-Berchem\",\n" +
            "                        \"stationinfo\": {\n" +
            "                            \"id\": \"BE.NMBS.008821121\",\n" +
            "                            \"locationX\": \"4.432221\",\n" +
            "                            \"locationY\": \"51.19923\",\n" +
            "                            \"@id\": \"http://irail.be/stations/NMBS/008821121\",\n" +
            "                            \"name\": \"Antwerp-Berchem\",\n" +
            "                            \"standardname\": \"Antwerpen-Berchem\"\n" +
            "                        },\n" +
            "                        \"vehicle\": \"BE.NMBS.IC1815\",\n" +
            "                        \"direction\": {\n" +
            "                            \"name\": \"Antwerpen-Centraal\"\n" +
            "                        }\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"id\": \"1\",\n" +
            "                        \"arrival\": {\n" +
            "                            \"time\": \"1510851120\",\n" +
            "                            \"platform\": \"6\",\n" +
            "                            \"platforminfo\": {\n" +
            "                                \"name\": \"6\",\n" +
            "                                \"normal\": \"1\"\n" +
            "                            },\n" +
            "                            \"isExtraStop\": \"0\",\n" +
            "                            \"delay\": \"0\",\n" +
            "                            \"canceled\": \"0\",\n" +
            "                            \"arrived\": \"0\",\n" +
            "                            \"walking\": \"0\",\n" +
            "                            \"direction\": {\n" +
            "                                \"name\": \"Hasselt\"\n" +
            "                            },\n" +
            "                            \"vehicle\": \"BE.NMBS.P8214\",\n" +
            "                            \"departureConnection\": \"http://irail.be/connections/8831005/20171116/P8214\"\n" +
            "                        },\n" +
            "                        \"departure\": {\n" +
            "                            \"time\": \"1510852560\",\n" +
            "                            \"platform\": \"2\",\n" +
            "                            \"platforminfo\": {\n" +
            "                                \"name\": \"2\",\n" +
            "                                \"normal\": \"1\"\n" +
            "                            },\n" +
            "                            \"isExtraStop\": \"0\",\n" +
            "                            \"delay\": \"0\",\n" +
            "                            \"canceled\": \"0\",\n" +
            "                            \"left\": \"0\",\n" +
            "                            \"walking\": \"0\",\n" +
            "                            \"direction\": {\n" +
            "                                \"name\": \"Genk\"\n" +
            "                            },\n" +
            "                            \"vehicle\": \"BE.NMBS.P2368\",\n" +
            "                            \"departureConnection\": \"http://irail.be/connections/8831005/20171116/P2368\",\n" +
            "                            \"occupancy\": {\n" +
            "                                \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                                \"name\": \"unknown\"\n" +
            "                            }\n" +
            "                        },\n" +
            "                        \"timeBetween\": \"1440\",\n" +
            "                        \"station\": \"Hasselt\",\n" +
            "                        \"stationinfo\": {\n" +
            "                            \"id\": \"BE.NMBS.008831005\",\n" +
            "                            \"locationX\": \"5.327627\",\n" +
            "                            \"locationY\": \"50.930822\",\n" +
            "                            \"@id\": \"http://irail.be/stations/NMBS/008831005\",\n" +
            "                            \"standardname\": \"Hasselt\",\n" +
            "                            \"name\": \"Hasselt\"\n" +
            "                        },\n" +
            "                        \"vehicle\": \"BE.NMBS.P8214\",\n" +
            "                        \"direction\": {\n" +
            "                            \"name\": \"Hasselt\"\n" +
            "                        }\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            \"occupancy\": {\n" +
            "                \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                \"name\": \"unknown\"\n" +
            "            }\n" +
            "        },\n" +
            "        {\n" +
            "            \"id\": \"4\",\n" +
            "            \"departure\": {\n" +
            "                \"delay\": \"0\",\n" +
            "                \"station\": \"Ghent-Dampoort\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008893120\",\n" +
            "                    \"locationX\": \"3.740591\",\n" +
            "                    \"locationY\": \"51.056365\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008893120\",\n" +
            "                    \"name\": \"Ghent-Dampoort\",\n" +
            "                    \"standardname\": \"Gent-Dampoort\"\n" +
            "                },\n" +
            "                \"time\": \"1510845840\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC715\",\n" +
            "                \"platform\": \"1\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"1\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8893120/20171116/IC715\",\n" +
            "                \"direction\": {\n" +
            "                    \"name\": \"Poperinge\"\n" +
            "                },\n" +
            "                \"left\": \"0\",\n" +
            "                \"walking\": \"0\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                    \"name\": \"unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            \"arrival\": {\n" +
            "                \"delay\": \"0\",\n" +
            "                \"station\": \"Kiewit\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008832375\",\n" +
            "                    \"locationX\": \"5.350226\",\n" +
            "                    \"locationY\": \"50.954841\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008832375\",\n" +
            "                    \"standardname\": \"Kiewit\",\n" +
            "                    \"name\": \"Kiewit\"\n" +
            "                },\n" +
            "                \"time\": \"1510854180\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC1515\",\n" +
            "                \"platform\": \"2\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"2\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"direction\": {\n" +
            "                    \"name\": \"Genk\"\n" +
            "                },\n" +
            "                \"arrived\": \"0\",\n" +
            "                \"walking\": \"0\"\n" +
            "            },\n" +
            "            \"duration\": \"8340\",\n" +
            "            \"alerts\": {\n" +
            "                \"number\": \"1\",\n" +
            "                \"alert\": [\n" +
            "                    {\n" +
            "                        \"id\": \"0\",\n" +
            "                        \"header\": \"Probleem bovenleiding  MIVB\",\n" +
            "                        \"description\": \"Reizigers tussen Brussel-Zuid en Brussel-Noord mogen met hun MIVB-ticket gebruik maken van de treinen van NMBS.\",\n" +
            "                        \"lead\": \"Probleem bovenleiding  MIVB\",\n" +
            "                        \"startTime\": \"1510834800\",\n" +
            "                        \"endTime\": \"1510873140\"\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            \"vias\": {\n" +
            "                \"number\": \"1\",\n" +
            "                \"via\": [\n" +
            "                    {\n" +
            "                        \"id\": \"0\",\n" +
            "                        \"arrival\": {\n" +
            "                            \"time\": \"1510846380\",\n" +
            "                            \"platform\": \"3\",\n" +
            "                            \"platforminfo\": {\n" +
            "                                \"name\": \"3\",\n" +
            "                                \"normal\": \"1\"\n" +
            "                            },\n" +
            "                            \"isExtraStop\": \"0\",\n" +
            "                            \"delay\": \"0\",\n" +
            "                            \"canceled\": \"0\",\n" +
            "                            \"arrived\": \"0\",\n" +
            "                            \"walking\": \"0\",\n" +
            "                            \"direction\": {\n" +
            "                                \"name\": \"Poperinge\"\n" +
            "                            },\n" +
            "                            \"vehicle\": \"BE.NMBS.IC715\",\n" +
            "                            \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC715\"\n" +
            "                        },\n" +
            "                        \"departure\": {\n" +
            "                            \"time\": \"1510846800\",\n" +
            "                            \"platform\": \"11\",\n" +
            "                            \"platforminfo\": {\n" +
            "                                \"name\": \"11\",\n" +
            "                                \"normal\": \"1\"\n" +
            "                            },\n" +
            "                            \"isExtraStop\": \"0\",\n" +
            "                            \"delay\": \"0\",\n" +
            "                            \"canceled\": \"0\",\n" +
            "                            \"alerts\": {\n" +
            "                                \"number\": \"1\",\n" +
            "                                \"alert\": [\n" +
            "                                    {\n" +
            "                                        \"id\": \"0\",\n" +
            "                                        \"header\": \"Probleem bovenleiding  MIVB\",\n" +
            "                                        \"description\": \"Reizigers tussen Brussel-Zuid en Brussel-Noord mogen met hun MIVB-ticket gebruik maken van de treinen van NMBS.\",\n" +
            "                                        \"lead\": \"Probleem bovenleiding  MIVB\",\n" +
            "                                        \"startTime\": \"1510834800\",\n" +
            "                                        \"endTime\": \"1510873140\"\n" +
            "                                    }\n" +
            "                                ]\n" +
            "                            },\n" +
            "                            \"left\": \"0\",\n" +
            "                            \"walking\": \"0\",\n" +
            "                            \"direction\": {\n" +
            "                                \"name\": \"Genk\"\n" +
            "                            },\n" +
            "                            \"vehicle\": \"BE.NMBS.IC1515\",\n" +
            "                            \"departureConnection\": \"http://irail.be/connections/8892007/20171116/IC1515\",\n" +
            "                            \"occupancy\": {\n" +
            "                                \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                                \"name\": \"unknown\"\n" +
            "                            }\n" +
            "                        },\n" +
            "                        \"timeBetween\": \"420\",\n" +
            "                        \"station\": \"Ghent-Sint-Pieters\",\n" +
            "                        \"stationinfo\": {\n" +
            "                            \"id\": \"BE.NMBS.008892007\",\n" +
            "                            \"locationX\": \"3.710675\",\n" +
            "                            \"locationY\": \"51.035896\",\n" +
            "                            \"@id\": \"http://irail.be/stations/NMBS/008892007\",\n" +
            "                            \"name\": \"Ghent-Sint-Pieters\",\n" +
            "                            \"standardname\": \"Gent-Sint-Pieters\"\n" +
            "                        },\n" +
            "                        \"vehicle\": \"BE.NMBS.IC715\",\n" +
            "                        \"direction\": {\n" +
            "                            \"name\": \"Poperinge\"\n" +
            "                        }\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            \"occupancy\": {\n" +
            "                \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                \"name\": \"unknown\"\n" +
            "            }\n" +
            "        },\n" +
            "        {\n" +
            "            \"id\": \"5\",\n" +
            "            \"departure\": {\n" +
            "                \"delay\": \"0\",\n" +
            "                \"station\": \"Ghent-Dampoort\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008893120\",\n" +
            "                    \"locationX\": \"3.740591\",\n" +
            "                    \"locationY\": \"51.056365\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008893120\",\n" +
            "                    \"name\": \"Ghent-Dampoort\",\n" +
            "                    \"standardname\": \"Gent-Dampoort\"\n" +
            "                },\n" +
            "                \"time\": \"1510848180\",\n" +
            "                \"vehicle\": \"BE.NMBS.IC1816\",\n" +
            "                \"platform\": \"2\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"2\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"departureConnection\": \"http://irail.be/connections/8893120/20171116/IC1816\",\n" +
            "                \"direction\": {\n" +
            "                    \"name\": \"Antwerpen-Centraal\"\n" +
            "                },\n" +
            "                \"left\": \"0\",\n" +
            "                \"walking\": \"0\",\n" +
            "                \"occupancy\": {\n" +
            "                    \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                    \"name\": \"unknown\"\n" +
            "                }\n" +
            "            },\n" +
            "            \"arrival\": {\n" +
            "                \"delay\": \"0\",\n" +
            "                \"station\": \"Kiewit\",\n" +
            "                \"stationinfo\": {\n" +
            "                    \"id\": \"BE.NMBS.008832375\",\n" +
            "                    \"locationX\": \"5.350226\",\n" +
            "                    \"locationY\": \"50.954841\",\n" +
            "                    \"@id\": \"http://irail.be/stations/NMBS/008832375\",\n" +
            "                    \"standardname\": \"Kiewit\",\n" +
            "                    \"name\": \"Kiewit\"\n" +
            "                },\n" +
            "                \"time\": \"1510855920\",\n" +
            "                \"vehicle\": \"BE.NMBS.P8303\",\n" +
            "                \"platform\": \"2\",\n" +
            "                \"platforminfo\": {\n" +
            "                    \"name\": \"2\",\n" +
            "                    \"normal\": \"1\"\n" +
            "                },\n" +
            "                \"canceled\": \"0\",\n" +
            "                \"direction\": {\n" +
            "                    \"name\": \"Genk\"\n" +
            "                },\n" +
            "                \"arrived\": \"0\",\n" +
            "                \"walking\": \"0\"\n" +
            "            },\n" +
            "            \"duration\": \"7740\",\n" +
            "            \"remarks\": {\n" +
            "                \"number\": \"2\",\n" +
            "                \"remark\": [\n" +
            "                    {\n" +
            "                        \"id\": \"0\",\n" +
            "                        \"code\": \"WAITING\",\n" +
            "                        \"description\": \"The connection of P   8215 will be guaranteed as long as the delay is less than 6 min.\"\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"id\": \"1\",\n" +
            "                        \"code\": \"WAITING\",\n" +
            "                        \"description\": \"The connection of P   8215 will be guaranteed as long as the delay is less than 6 min.\"\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            \"vias\": {\n" +
            "                \"number\": \"2\",\n" +
            "                \"via\": [\n" +
            "                    {\n" +
            "                        \"id\": \"0\",\n" +
            "                        \"arrival\": {\n" +
            "                            \"time\": \"1510850880\",\n" +
            "                            \"platform\": \"7\",\n" +
            "                            \"platforminfo\": {\n" +
            "                                \"name\": \"7\",\n" +
            "                                \"normal\": \"1\"\n" +
            "                            },\n" +
            "                            \"isExtraStop\": \"0\",\n" +
            "                            \"delay\": \"0\",\n" +
            "                            \"canceled\": \"0\",\n" +
            "                            \"arrived\": \"0\",\n" +
            "                            \"walking\": \"0\",\n" +
            "                            \"direction\": {\n" +
            "                                \"name\": \"Antwerpen-Centraal\"\n" +
            "                            },\n" +
            "                            \"vehicle\": \"BE.NMBS.IC1816\",\n" +
            "                            \"departureConnection\": \"http://irail.be/connections/8821121/20171116/IC1816\"\n" +
            "                        },\n" +
            "                        \"departure\": {\n" +
            "                            \"time\": \"1510851420\",\n" +
            "                            \"platform\": \"5\",\n" +
            "                            \"platforminfo\": {\n" +
            "                                \"name\": \"5\",\n" +
            "                                \"normal\": \"1\"\n" +
            "                            },\n" +
            "                            \"isExtraStop\": \"0\",\n" +
            "                            \"delay\": \"0\",\n" +
            "                            \"canceled\": \"0\",\n" +
            "                            \"left\": \"0\",\n" +
            "                            \"walking\": \"0\",\n" +
            "                            \"direction\": {\n" +
            "                                \"name\": \"Hasselt\"\n" +
            "                            },\n" +
            "                            \"vehicle\": \"BE.NMBS.P8215\",\n" +
            "                            \"departureConnection\": \"http://irail.be/connections/8821121/20171116/P8215\",\n" +
            "                            \"occupancy\": {\n" +
            "                                \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                                \"name\": \"unknown\"\n" +
            "                            }\n" +
            "                        },\n" +
            "                        \"timeBetween\": \"540\",\n" +
            "                        \"station\": \"Antwerp-Berchem\",\n" +
            "                        \"stationinfo\": {\n" +
            "                            \"id\": \"BE.NMBS.008821121\",\n" +
            "                            \"locationX\": \"4.432221\",\n" +
            "                            \"locationY\": \"51.19923\",\n" +
            "                            \"@id\": \"http://irail.be/stations/NMBS/008821121\",\n" +
            "                            \"name\": \"Antwerp-Berchem\",\n" +
            "                            \"standardname\": \"Antwerpen-Berchem\"\n" +
            "                        },\n" +
            "                        \"vehicle\": \"BE.NMBS.IC1816\",\n" +
            "                        \"direction\": {\n" +
            "                            \"name\": \"Antwerpen-Centraal\"\n" +
            "                        }\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"id\": \"1\",\n" +
            "                        \"arrival\": {\n" +
            "                            \"time\": \"1510854720\",\n" +
            "                            \"platform\": \"7\",\n" +
            "                            \"platforminfo\": {\n" +
            "                                \"name\": \"7\",\n" +
            "                                \"normal\": \"1\"\n" +
            "                            },\n" +
            "                            \"isExtraStop\": \"0\",\n" +
            "                            \"delay\": \"0\",\n" +
            "                            \"canceled\": \"0\",\n" +
            "                            \"arrived\": \"0\",\n" +
            "                            \"walking\": \"0\",\n" +
            "                            \"direction\": {\n" +
            "                                \"name\": \"Hasselt\"\n" +
            "                            },\n" +
            "                            \"vehicle\": \"BE.NMBS.P8215\",\n" +
            "                            \"departureConnection\": \"http://irail.be/connections/8831005/20171116/P8215\"\n" +
            "                        },\n" +
            "                        \"departure\": {\n" +
            "                            \"time\": \"1510855440\",\n" +
            "                            \"platform\": \"8\",\n" +
            "                            \"platforminfo\": {\n" +
            "                                \"name\": \"8\",\n" +
            "                                \"normal\": \"1\"\n" +
            "                            },\n" +
            "                            \"isExtraStop\": \"0\",\n" +
            "                            \"delay\": \"0\",\n" +
            "                            \"canceled\": \"0\",\n" +
            "                            \"left\": \"0\",\n" +
            "                            \"walking\": \"0\",\n" +
            "                            \"direction\": {\n" +
            "                                \"name\": \"Genk\"\n" +
            "                            },\n" +
            "                            \"vehicle\": \"BE.NMBS.P8303\",\n" +
            "                            \"departureConnection\": \"http://irail.be/connections/8831005/20171116/P8303\",\n" +
            "                            \"occupancy\": {\n" +
            "                                \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                                \"name\": \"unknown\"\n" +
            "                            }\n" +
            "                        },\n" +
            "                        \"timeBetween\": \"720\",\n" +
            "                        \"station\": \"Hasselt\",\n" +
            "                        \"stationinfo\": {\n" +
            "                            \"id\": \"BE.NMBS.008831005\",\n" +
            "                            \"locationX\": \"5.327627\",\n" +
            "                            \"locationY\": \"50.930822\",\n" +
            "                            \"@id\": \"http://irail.be/stations/NMBS/008831005\",\n" +
            "                            \"standardname\": \"Hasselt\",\n" +
            "                            \"name\": \"Hasselt\"\n" +
            "                        },\n" +
            "                        \"vehicle\": \"BE.NMBS.P8215\",\n" +
            "                        \"direction\": {\n" +
            "                            \"name\": \"Hasselt\"\n" +
            "                        }\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            \"occupancy\": {\n" +
            "                \"@id\": \"http://api.irail.be/terms/unknown\",\n" +
            "                \"name\": \"unknown\"\n" +
            "            }\n" +
            "        }\n" +
            "    ]\n" +
            "\n" +
            "}";
}
