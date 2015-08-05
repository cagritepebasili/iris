package net.contargo.iris.enricher.service;

import net.contargo.iris.GeoLocation;
import net.contargo.iris.address.nominatim.service.AddressResolutionException;
import net.contargo.iris.location.GeoLocationService;
import net.contargo.iris.route.RoutePart;
import net.contargo.iris.route.SubRoutePart;
import net.contargo.iris.terminal.Terminal;

import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;

import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import static net.contargo.iris.route.RouteType.BARGE_RAIL;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import static org.mockito.Mockito.when;

import static java.util.Collections.singletonList;


/**
 * Unit test of {@link net.contargo.iris.enricher.service.GeoLocationPartEnricher}.
 *
 * @author  Tobias Schneider - schneider@synyx.de
 */
@RunWith(MockitoJUnitRunner.class)
public class GeoLocationPartEnricherUnitTest {

    private GeoLocationPartEnricher sut;

    @Mock
    private GeoLocationService geoLocationServiceMock;
    private RoutePart routePart;

    // This is no mock because it is final
    private EnricherContext enricherContext;
    private GeoLocation x;
    private GeoLocation y;

    @Before
    public void setup() {

        routePart = new RoutePart();

        x = new GeoLocation();
        y = new GeoLocation();

        routePart.setOrigin(x);
        routePart.setDestination(y);

        sut = new GeoLocationPartEnricher(geoLocationServiceMock);

        enricherContext = new EnricherContext.Builder().build();
    }


    @Test
    public void testDelegation() throws CriticalEnricherException {

        Terminal t = new Terminal();
        Terminal t2 = new Terminal();

        when(geoLocationServiceMock.getDetailedGeoLocation(x)).thenReturn(t);
        when(geoLocationServiceMock.getDetailedGeoLocation(y)).thenReturn(t2);

        sut.enrich(routePart, enricherContext);

        assertThat(routePart.getOrigin(), is(t));
        assertThat(routePart.getDestination(), is(t2));
    }


    @Test(expected = CriticalEnricherException.class)
    public void enrichWithCriticalException() throws CriticalEnricherException {

        when(geoLocationServiceMock.getDetailedGeoLocation(x)).thenThrow(new AddressResolutionException("",
                new Throwable()));

        sut.enrich(routePart, enricherContext);
    }


    @Test
    public void enrichWithSubRouteParts() throws CriticalEnricherException {

        SubRoutePart subRoutePart = new SubRoutePart();
        subRoutePart.setOrigin(x);
        subRoutePart.setDestination(y);

        Terminal t = new Terminal();
        Terminal t2 = new Terminal();

        routePart.setRouteType(BARGE_RAIL);
        routePart.setSubRouteParts(singletonList(subRoutePart));

        when(geoLocationServiceMock.getDetailedGeoLocation(x)).thenReturn(t);
        when(geoLocationServiceMock.getDetailedGeoLocation(y)).thenReturn(t2);

        sut.enrich(routePart, enricherContext);

        assertThat(routePart.getSubRouteParts(), hasSize(1));
        assertThat(routePart.getSubRouteParts().get(0).getOrigin(), is(t));
        assertThat(routePart.getSubRouteParts().get(0).getDestination(), is(t2));
    }
}
