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

import static be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition.ARRIVE_AT;
import static be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition.DEPART_AT;
import static be.hyperrail.opentransportdata.common.models.LiveboardType.ARRIVALS;
import static be.hyperrail.opentransportdata.common.models.LiveboardType.DEPARTURES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Created in be.hyperrail.android.irail.implementation on 22/04/2018.
 */
class LiveboardImplTest {

    @Test
    void getSearchTime_shouldReturnCorrectValue() {
        DateTime mDateTime = DateTime.now();
        StopLocation station = Mockito.mock(StopLocation.class);
        Mockito.when(station.getHafasId()).thenReturn("008814001");
        VehicleStop stop = Mockito.mock(VehicleStop.class);
        VehicleStop[] stops = new VehicleStop[]{stop};
        Liveboard instance = new LiveboardImpl(station, stops, mDateTime, DEPARTURES, DEPART_AT);

        assertEquals(mDateTime, instance.getSearchTime());
    }

    @Test
    void getTimeDefinition_shouldReturnCorrectValue() {
        DateTime mDateTime = DateTime.now();
        StopLocation station = Mockito.mock(StopLocation.class);
        Mockito.when(station.getHafasId()).thenReturn("008814001");
        VehicleStop stop = Mockito.mock(VehicleStop.class);
        VehicleStop[] stops = new VehicleStop[]{stop};
        Liveboard departing = new LiveboardImpl(station, stops, mDateTime, DEPARTURES, DEPART_AT);
        Liveboard arriving = new LiveboardImpl(station, stops, mDateTime, DEPARTURES, QueryTimeDefinition.ARRIVE_AT);

        assertEquals(DEPART_AT, departing.getTimeDefinition());
        assertEquals(QueryTimeDefinition.ARRIVE_AT, arriving.getTimeDefinition());
    }

    @Test
    void getLiveboardType_shouldReturnCorrectValue() {
        DateTime mDateTime = DateTime.now();
        StopLocation station = Mockito.mock(StopLocation.class);
        Mockito.when(station.getHafasId()).thenReturn("008814001");
        VehicleStop stop = Mockito.mock(VehicleStop.class);
        VehicleStop[] stops = new VehicleStop[]{stop};
        Liveboard departing = new LiveboardImpl(station, stops, mDateTime, DEPARTURES, DEPART_AT);
        Liveboard arriving = new LiveboardImpl(station, stops, mDateTime, LiveboardType.ARRIVALS, DEPART_AT);

        assertEquals(DEPARTURES, departing.getLiveboardType());
        assertEquals(LiveboardType.ARRIVALS, arriving.getLiveboardType());
    }

    @Test
    void withStopsAppended_stopsAfterLastDepartureInOriginalLiveboard_shouldAppendCorrectly() {
        DateTime mDateTimeOriginal = DateTime.now();
        DateTime mDateTimeToAppend = DateTime.now().plusHours(1);
        StopLocation station = Mockito.mock(StopLocation.class);
        Mockito.when(station.getHafasId()).thenReturn("008814001");

        VehicleStop firstOriginalStop = Mockito.mock(VehicleStopImpl.class);
        Mockito.when(firstOriginalStop.getDepartureUri()).thenReturn("http://mockito/stop1");
        Mockito.when(firstOriginalStop.getDepartureTime()).thenReturn(new DateTime(2019, 2, 1, 10, 0));

        VehicleStop secondOriginalStop = Mockito.mock(VehicleStopImpl.class);
        Mockito.when(secondOriginalStop.getDepartureUri()).thenReturn("http://mockito/stop2");
        Mockito.when(secondOriginalStop.getDepartureTime()).thenReturn(new DateTime(2019, 2, 1, 10, 10));

        VehicleStop firstStopToAppend = Mockito.mock(VehicleStopImpl.class);
        Mockito.when(firstStopToAppend.getDepartureUri()).thenReturn("http://mockito/stop3");
        Mockito.when(firstStopToAppend.getDepartureTime()).thenReturn(new DateTime(2019, 2, 1, 10, 10));

        VehicleStop secondStopToAppend = Mockito.mock(VehicleStopImpl.class);
        Mockito.when(secondStopToAppend.getDepartureUri()).thenReturn("http://mockito/stop4");
        Mockito.when(secondStopToAppend.getDepartureTime()).thenReturn(new DateTime(2019, 2, 1, 10, 20));

        VehicleStop[] originalStops = new VehicleStop[]{firstOriginalStop, secondOriginalStop};
        LiveboardImpl original = new LiveboardImpl(station, originalStops, mDateTimeOriginal, DEPARTURES, DEPART_AT);

        VehicleStop[] stopsToAppend = new VehicleStop[]{firstStopToAppend, secondStopToAppend};
        LiveboardImpl dataToAppend = new LiveboardImpl(station, stopsToAppend, mDateTimeToAppend, DEPARTURES, DEPART_AT);

        LiveboardImpl combined = original.withStopsAppended(dataToAppend);

        assertEquals(original.getSearchTime(), combined.getSearchTime());
        assertNotEquals(dataToAppend.getSearchTime(), combined.getCurrentResultsPointer());

        assertEquals(DEPARTURES, combined.getLiveboardType());
        assertEquals(DEPART_AT, combined.getTimeDefinition());

        assertEquals(4, combined.getStops().length);
        assertEquals(firstOriginalStop, combined.getStops()[0]);
        assertEquals(secondOriginalStop, combined.getStops()[1]);
        assertEquals(firstStopToAppend, combined.getStops()[2]);
        assertEquals(secondStopToAppend, combined.getStops()[3]);
    }

    @Test
    void withStopsAppended_stopsBeforeAndAfterLastDepartureInOriginalLiveboard_shouldAppendCorrectly() {
        DateTime mDateTimeOriginal = DateTime.now();
        DateTime mDateTimeToAppend = DateTime.now().plusHours(1);
        StopLocation station = Mockito.mock(StopLocation.class);
        Mockito.when(station.getHafasId()).thenReturn("008814001");

        VehicleStop firstOriginalStop = Mockito.mock(VehicleStopImpl.class);
        Mockito.when(firstOriginalStop.getDepartureUri()).thenReturn("http://mockito/stop1");
        Mockito.when(firstOriginalStop.getDepartureTime()).thenReturn(new DateTime(2019, 2, 1, 10, 0));

        VehicleStop secondOriginalStop = Mockito.mock(VehicleStopImpl.class);
        Mockito.when(secondOriginalStop.getDepartureUri()).thenReturn("http://mockito/stop2");
        Mockito.when(secondOriginalStop.getDepartureTime()).thenReturn(new DateTime(2019, 2, 1, 10, 10));

        VehicleStop firstStopToAppend = Mockito.mock(VehicleStopImpl.class);
        Mockito.when(firstStopToAppend.getDepartureUri()).thenReturn("http://mockito/stop3");
        Mockito.when(firstStopToAppend.getDepartureTime()).thenReturn(new DateTime(2019, 2, 1, 10, 5));

        VehicleStop secondStopToAppend = Mockito.mock(VehicleStopImpl.class);
        Mockito.when(secondStopToAppend.getDepartureUri()).thenReturn("http://mockito/stop4");
        Mockito.when(secondStopToAppend.getDepartureTime()).thenReturn(new DateTime(2019, 2, 1, 10, 20));

        VehicleStop[] originalStops = new VehicleStop[]{firstOriginalStop, secondOriginalStop};
        LiveboardImpl original = new LiveboardImpl(station, originalStops, mDateTimeOriginal, DEPARTURES, DEPART_AT);

        VehicleStop[] stopsToAppend = new VehicleStop[]{firstStopToAppend, secondStopToAppend};
        LiveboardImpl dataToAppend = new LiveboardImpl(station, stopsToAppend, mDateTimeToAppend, DEPARTURES, DEPART_AT);

        LiveboardImpl combined = original.withStopsAppended(dataToAppend);

        assertEquals(original.getSearchTime(), combined.getSearchTime());
        assertNotEquals(dataToAppend.getSearchTime(), combined.getCurrentResultsPointer());

        assertEquals(DEPARTURES, combined.getLiveboardType());
        assertEquals(DEPART_AT, combined.getTimeDefinition());

        assertEquals(4, combined.getStops().length);
        assertEquals(firstOriginalStop, combined.getStops()[0]);
        assertEquals(firstStopToAppend, combined.getStops()[1]);
        assertEquals(secondOriginalStop, combined.getStops()[2]);
        assertEquals(secondStopToAppend, combined.getStops()[3]);
    }

    @Test
    void withStopsAppended_stopsAfterLastArrivalInOriginalLiveboard_shouldAppendCorrectly() {
        DateTime mDateTimeOriginal = DateTime.now();
        DateTime mDateTimeToAppend = DateTime.now().plusHours(1);
        StopLocation station = Mockito.mock(StopLocation.class);
        Mockito.when(station.getHafasId()).thenReturn("008814001");

        VehicleStop firstOriginalStop = Mockito.mock(VehicleStopImpl.class);
        Mockito.when(firstOriginalStop.getDepartureUri()).thenReturn("http://mockito/stop1");
        Mockito.when(firstOriginalStop.getArrivalTime()).thenReturn(new DateTime(2019, 2, 1, 10, 0));

        VehicleStop secondOriginalStop = Mockito.mock(VehicleStopImpl.class);
        Mockito.when(secondOriginalStop.getDepartureUri()).thenReturn("http://mockito/stop2");
        Mockito.when(secondOriginalStop.getArrivalTime()).thenReturn(new DateTime(2019, 2, 1, 10, 10));

        VehicleStop firstStopToAppend = Mockito.mock(VehicleStopImpl.class);
        Mockito.when(firstStopToAppend.getDepartureUri()).thenReturn("http://mockito/stop3");
        Mockito.when(firstStopToAppend.getArrivalTime()).thenReturn(new DateTime(2019, 2, 1, 10, 10));

        VehicleStop secondStopToAppend = Mockito.mock(VehicleStopImpl.class);
        Mockito.when(secondStopToAppend.getDepartureUri()).thenReturn("http://mockito/stop4");
        Mockito.when(secondStopToAppend.getArrivalTime()).thenReturn(new DateTime(2019, 2, 1, 10, 20));

        VehicleStop[] originalStops = new VehicleStop[]{firstOriginalStop, secondOriginalStop};
        LiveboardImpl original = new LiveboardImpl(station, originalStops, mDateTimeOriginal, ARRIVALS, ARRIVE_AT);

        VehicleStop[] stopsToAppend = new VehicleStop[]{firstStopToAppend, secondStopToAppend};
        LiveboardImpl dataToAppend = new LiveboardImpl(station, stopsToAppend, mDateTimeToAppend, ARRIVALS, ARRIVE_AT);

        LiveboardImpl combined = original.withStopsAppended(dataToAppend);

        assertEquals(original.getSearchTime(), combined.getSearchTime());
        assertNotEquals(dataToAppend.getSearchTime(), combined.getCurrentResultsPointer());

        assertEquals(ARRIVALS, combined.getLiveboardType());
        assertEquals(ARRIVE_AT, combined.getTimeDefinition());

        assertEquals(4, combined.getStops().length);
        assertEquals(firstOriginalStop, combined.getStops()[0]);
        assertEquals(secondOriginalStop, combined.getStops()[1]);
        assertEquals(firstStopToAppend, combined.getStops()[2]);
        assertEquals(secondStopToAppend, combined.getStops()[3]);
    }

    @Test
    void withStopsAppended_stopsBeforeAndAfterLastArrivalInOriginalLiveboard_shouldAppendCorrectly() {
        DateTime mDateTimeOriginal = DateTime.now();
        DateTime mDateTimeToAppend = DateTime.now().plusHours(1);
        StopLocation station = Mockito.mock(StopLocation.class);
        Mockito.when(station.getHafasId()).thenReturn("008814001");

        VehicleStop firstOriginalStop = Mockito.mock(VehicleStopImpl.class);
        Mockito.when(firstOriginalStop.getDepartureUri()).thenReturn("http://mockito/stop1");
        Mockito.when(firstOriginalStop.getArrivalTime()).thenReturn(new DateTime(2019, 2, 1, 10, 0));

        VehicleStop secondOriginalStop = Mockito.mock(VehicleStopImpl.class);
        Mockito.when(secondOriginalStop.getDepartureUri()).thenReturn("http://mockito/stop2");
        Mockito.when(secondOriginalStop.getArrivalTime()).thenReturn(new DateTime(2019, 2, 1, 10, 10));

        VehicleStop firstStopToAppend = Mockito.mock(VehicleStopImpl.class);
        Mockito.when(firstStopToAppend.getDepartureUri()).thenReturn("http://mockito/stop3");
        Mockito.when(firstStopToAppend.getArrivalTime()).thenReturn(new DateTime(2019, 2, 1, 10, 5));

        VehicleStop secondStopToAppend = Mockito.mock(VehicleStopImpl.class);
        Mockito.when(secondStopToAppend.getDepartureUri()).thenReturn("http://mockito/stop4");
        Mockito.when(secondStopToAppend.getArrivalTime()).thenReturn(new DateTime(2019, 2, 1, 10, 20));

        VehicleStop[] originalStops = new VehicleStop[]{firstOriginalStop, secondOriginalStop};
        LiveboardImpl original = new LiveboardImpl(station, originalStops, mDateTimeOriginal, ARRIVALS, ARRIVE_AT);

        VehicleStop[] stopsToAppend = new VehicleStop[]{firstStopToAppend, secondStopToAppend};
        LiveboardImpl dataToAppend = new LiveboardImpl(station, stopsToAppend, mDateTimeToAppend, ARRIVALS, ARRIVE_AT);

        LiveboardImpl combined = original.withStopsAppended(dataToAppend);

        assertEquals(original.getSearchTime(), combined.getSearchTime());
        assertNotEquals(dataToAppend.getSearchTime(), combined.getCurrentResultsPointer());

        assertEquals(ARRIVALS, combined.getLiveboardType());
        assertEquals(ARRIVE_AT, combined.getTimeDefinition());

        assertEquals(4, combined.getStops().length);
        assertEquals(firstOriginalStop, combined.getStops()[0]);
        assertEquals(firstStopToAppend, combined.getStops()[1]);
        assertEquals(secondOriginalStop, combined.getStops()[2]);
        assertEquals(secondStopToAppend, combined.getStops()[3]);
    }

}