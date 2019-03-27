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

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition;
import be.hyperrail.opentransportdata.common.models.Liveboard;
import be.hyperrail.opentransportdata.common.models.LiveboardType;
import be.hyperrail.opentransportdata.common.models.StopLocation;
import be.hyperrail.opentransportdata.common.models.VehicleStop;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created in be.hyperrail.android.irail.implementation on 22/04/2018.
 */
class LiveboardImplTest {

    @Test
    void getSearchTime() {
        DateTime mDateTime = DateTime.now();
        StopLocation station = Mockito.mock(StopLocation.class);
        Mockito.when(station.getHafasId()).thenReturn("008814001");
        VehicleStop stop = Mockito.mock(VehicleStop.class);
        VehicleStop[] stops = new VehicleStop[]{stop};
        Liveboard instance = new LiveboardImpl(station, stops, mDateTime, LiveboardType.DEPARTURES, QueryTimeDefinition.DEPART_AT);

        assertEquals(mDateTime, instance.getSearchTime());
    }

    @Test
    void getTimeDefinition() {
        DateTime mDateTime = DateTime.now();
        StopLocation station = Mockito.mock(StopLocation.class);
        Mockito.when(station.getHafasId()).thenReturn("008814001");
        VehicleStop stop = Mockito.mock(VehicleStop.class);
        VehicleStop[] stops = new VehicleStop[]{stop};
        Liveboard departing = new LiveboardImpl(station, stops, mDateTime, LiveboardType.DEPARTURES, QueryTimeDefinition.DEPART_AT);
        Liveboard arriving = new LiveboardImpl(station, stops, mDateTime, LiveboardType.DEPARTURES, QueryTimeDefinition.ARRIVE_AT);

        assertEquals(QueryTimeDefinition.DEPART_AT, departing.getTimeDefinition());
        assertEquals(QueryTimeDefinition.ARRIVE_AT, arriving.getTimeDefinition());
    }

    @Test
    void getLiveboardType() {
        DateTime mDateTime = DateTime.now();
        StopLocation station = Mockito.mock(StopLocation.class);
        Mockito.when(station.getHafasId()).thenReturn("008814001");
        VehicleStop stop = Mockito.mock(VehicleStop.class);
        VehicleStop[] stops = new VehicleStop[]{stop};
        Liveboard departing = new LiveboardImpl(station, stops, mDateTime, LiveboardType.DEPARTURES, QueryTimeDefinition.DEPART_AT);
        Liveboard arriving = new LiveboardImpl(station, stops, mDateTime, LiveboardType.ARRIVALS, QueryTimeDefinition.DEPART_AT);

        assertEquals(LiveboardType.DEPARTURES, departing.getLiveboardType());
        assertEquals(LiveboardType.ARRIVALS, arriving.getLiveboardType());
    }

    @Test
    void withStopsAppended() {
        DateTime mDateTime = DateTime.now();
        StopLocation station = Mockito.mock(StopLocation.class);
        Mockito.when(station.getHafasId()).thenReturn("008814001");
        VehicleStop stop = Mockito.mock(VehicleStop.class);
        VehicleStop[] stops = new VehicleStop[]{stop};
        Liveboard initial = new LiveboardImpl(station, stops, mDateTime, LiveboardType.DEPARTURES, QueryTimeDefinition.DEPART_AT);

        // TODO: test logic
    }

}