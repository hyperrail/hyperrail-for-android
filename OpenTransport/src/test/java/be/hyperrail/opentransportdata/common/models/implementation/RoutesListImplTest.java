package be.hyperrail.opentransportdata.common.models.implementation;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import be.hyperrail.opentransportdata.common.contracts.NextDataPointer;
import be.hyperrail.opentransportdata.common.contracts.QueryTimeDefinition;
import be.hyperrail.opentransportdata.common.models.Route;
import be.hyperrail.opentransportdata.common.models.RoutesList;
import be.hyperrail.opentransportdata.common.models.StopLocation;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoutesListImplTest {
    @Test
    void gettersAndSetter_shouldWorkAsExpected() {

        StopLocation firstStation = Mockito.mock(StopLocation.class);
        StopLocation secondStation = Mockito.mock(StopLocation.class);
        Route[] routes = new Route[]{Mockito.mock(Route.class)};
        DateTime now = DateTime.now();
        RoutesList list = new RoutesListImpl(firstStation, secondStation, now, QueryTimeDefinition.DEPART_AT, routes);

        assertEquals(firstStation, list.getOrigin());
        assertEquals(secondStation, list.getDestination());
        assertEquals(routes, list.getRoutes());
        assertEquals(now, list.getSearchTime());
    }
    @Test
    void pagination_shouldWorkAsExpected() {

        StopLocation firstStation = Mockito.mock(StopLocation.class);
        StopLocation secondStation = Mockito.mock(StopLocation.class);
        Route[] routes = new Route[]{Mockito.mock(Route.class)};
        DateTime now = DateTime.now();
        RoutesListImpl list = new RoutesListImpl(firstStation, secondStation, now, QueryTimeDefinition.DEPART_AT, routes);

        NextDataPointer next = Mockito.mock(NextDataPointer.class);
        NextDataPointer current = Mockito.mock(NextDataPointer.class);
        NextDataPointer previous = Mockito.mock(NextDataPointer.class);

         list.setPageInfo(previous,current,next);

         assertEquals(previous, list.getPreviousResultsPointer());
         assertEquals(current, list.getCurrentResultsPointer());
         assertEquals(next, list.getNextResultsPointer());
    }

}