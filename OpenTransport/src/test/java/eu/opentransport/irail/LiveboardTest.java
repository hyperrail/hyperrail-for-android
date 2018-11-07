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

import org.joda.time.DateTime;
import org.junit.Test;

import be.hyperrail.android.irail.contracts.RouteTimeDefinition;
import be.hyperrail.android.irail.db.Station;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created in be.hyperrail.android.irail.implementation on 22/04/2018.
 */
public class LiveboardTest {

    @Test
    public void getSearchTime() {
        DateTime mDateTime = DateTime.now();
        Station station = Mockito.mock(Station.class);
        Mockito.when(station.getHafasId()).thenReturn("008814001");
        VehicleStop stop = Mockito.mock(VehicleStop.class);
        VehicleStop[] stops = new VehicleStop[]{stop};
        Liveboard instance = new Liveboard(station, stops, mDateTime, Liveboard.LiveboardType.DEPARTURES, RouteTimeDefinition.DEPART_AT);

        assertEquals(mDateTime, instance.getSearchTime());
    }

    @Test
    public void getTimeDefinition() {
        DateTime mDateTime = DateTime.now();
        Station station = Mockito.mock(Station.class);
        Mockito.when(station.getHafasId()).thenReturn("008814001");
        VehicleStop stop = Mockito.mock(VehicleStop.class);
        VehicleStop[] stops = new VehicleStop[]{stop};
        Liveboard departing = new Liveboard(station, stops, mDateTime, Liveboard.LiveboardType.DEPARTURES, RouteTimeDefinition.DEPART_AT);
        Liveboard arriving = new Liveboard(station, stops, mDateTime, Liveboard.LiveboardType.DEPARTURES, RouteTimeDefinition.ARRIVE_AT);

        assertEquals(RouteTimeDefinition.DEPART_AT, departing.getTimeDefinition());
        assertEquals(RouteTimeDefinition.ARRIVE_AT, arriving.getTimeDefinition());
    }

    @Test
    public void getLiveboardType() {
        DateTime mDateTime = DateTime.now();
        Station station = Mockito.mock(Station.class);
        Mockito.when(station.getHafasId()).thenReturn("008814001");
        VehicleStop stop = Mockito.mock(VehicleStop.class);
        VehicleStop[] stops = new VehicleStop[]{stop};
        Liveboard departing = new Liveboard(station, stops, mDateTime, Liveboard.LiveboardType.DEPARTURES, RouteTimeDefinition.DEPART_AT);
        Liveboard arriving = new Liveboard(station, stops, mDateTime, Liveboard.LiveboardType.ARRIVALS, RouteTimeDefinition.DEPART_AT);

        assertEquals(Liveboard.LiveboardType.DEPARTURES, departing.getLiveboardType());
        assertEquals(Liveboard.LiveboardType.ARRIVALS, arriving.getLiveboardType());
    }

    @Test
    public void withStopsAppended() {
        DateTime mDateTime = DateTime.now();
        Station station = Mockito.mock(Station.class);
        Mockito.when(station.getHafasId()).thenReturn("008814001");
        VehicleStop stop = Mockito.mock(VehicleStop.class);
        VehicleStop[] stops = new VehicleStop[]{stop};
        Liveboard initial = new Liveboard(station, stops, mDateTime, Liveboard.LiveboardType.DEPARTURES, RouteTimeDefinition.DEPART_AT);

        // TODO: test logic
    }

}