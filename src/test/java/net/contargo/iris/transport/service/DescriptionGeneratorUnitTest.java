package net.contargo.iris.transport.service;

import net.contargo.iris.GeoLocation;
import net.contargo.iris.connection.MainRunConnection;
import net.contargo.iris.connection.service.MainRunConnectionService;
import net.contargo.iris.route.RouteType;
import net.contargo.iris.seaport.Seaport;
import net.contargo.iris.terminal.Terminal;
import net.contargo.iris.terminal.service.TerminalService;
import net.contargo.iris.transport.api.ModeOfTransport;
import net.contargo.iris.transport.api.TransportDescriptionDto;
import net.contargo.iris.transport.api.TransportTemplateDto;

import org.junit.Test;

import org.junit.runner.RunWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.util.List;

import static net.contargo.iris.container.ContainerState.EMPTY;
import static net.contargo.iris.container.ContainerState.FULL;
import static net.contargo.iris.route.RouteType.BARGE;
import static net.contargo.iris.route.RouteType.RAIL;
import static net.contargo.iris.transport.api.SiteType.ADDRESS;
import static net.contargo.iris.transport.api.SiteType.SEAPORT;
import static net.contargo.iris.transport.api.SiteType.TERMINAL;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import static org.mockito.Mockito.when;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TEN;
import static java.math.BigDecimal.ZERO;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;


/**
 * @author  Sandra Thieme - thieme@synyx.de
 * @author  Ben Antony - antony@synyx.de
 */
@RunWith(MockitoJUnitRunner.class)
public class DescriptionGeneratorUnitTest {

    @InjectMocks
    private DescriptionGenerator sut;

    @Mock
    private TerminalService terminalServiceMock;
    @Mock
    private MainRunConnectionService connectionServiceMock;

    @Test
    public void onewayImport() {

        Seaport antwerp = seaport("111");

        Terminal woerth = terminal("1234565789", TEN, TEN);
        Terminal malu = terminal("987654321", TEN, ZERO);

        when(terminalServiceMock.getAllActive()).thenReturn(asList(woerth, malu));

        MainRunConnection antwerpWoerthWater = connection(antwerp, woerth, BARGE);
        MainRunConnection antwerpWoerthRail = connection(antwerp, woerth, RAIL);

        when(connectionServiceMock.getConnectionsForTerminal(new BigInteger("1234565789"))).thenReturn(asList(
                antwerpWoerthWater, antwerpWoerthRail));

        MainRunConnection antwerpMaluWater = connection(antwerp, malu, BARGE);

        when(connectionServiceMock.getConnectionsForTerminal(new BigInteger("987654321"))).thenReturn(singletonList(
                antwerpMaluWater));

        TransportTemplateDto.TransportSegment antwerpTerminal = new TransportTemplateDto.TransportSegment(
                new TransportTemplateDto.TransportSite(SEAPORT, "111", null, null),
                new TransportTemplateDto.TransportSite(TERMINAL, null, null, null), FULL, true, null);
        TransportTemplateDto.TransportSegment terminalAddress = new TransportTemplateDto.TransportSegment(
                new TransportTemplateDto.TransportSite(TERMINAL, null, null, null),
                new TransportTemplateDto.TransportSite(ADDRESS, null, ONE, TEN), FULL, true, null);
        TransportTemplateDto.TransportSegment addressTerminal = new TransportTemplateDto.TransportSegment(
                new TransportTemplateDto.TransportSite(ADDRESS, null, ONE, TEN),
                new TransportTemplateDto.TransportSite(TERMINAL, null, null, null), EMPTY, true, null);
        TransportTemplateDto template = new TransportTemplateDto(asList(antwerpTerminal, terminalAddress,
                    addressTerminal));

        List<TransportDescriptionDto> descriptions = sut.from(template);

        assertThat(descriptions, hasSize(3));

        assertThat(descriptions.get(0).transportDescription.get(0).modeOfTransport, is(ModeOfTransport.WATER));
        assertThat(descriptions.get(0).transportDescription.get(0).toSite.uuid, is("1234565789"));
        assertThat(descriptions.get(0).transportDescription.get(1).fromSite.uuid, is("1234565789"));
        assertThat(descriptions.get(0).transportDescription.get(1).modeOfTransport, is(ModeOfTransport.ROAD));
        assertThat(descriptions.get(0).transportDescription.get(2).toSite.uuid, is("1234565789"));
        assertThat(descriptions.get(0).transportDescription.get(2).modeOfTransport, is(ModeOfTransport.ROAD));

        assertThat(descriptions.get(1).transportDescription.get(0).modeOfTransport, is(ModeOfTransport.RAIL));
        assertThat(descriptions.get(1).transportDescription.get(0).toSite.uuid, is("1234565789"));
        assertThat(descriptions.get(1).transportDescription.get(1).fromSite.uuid, is("1234565789"));
        assertThat(descriptions.get(1).transportDescription.get(1).modeOfTransport, is(ModeOfTransport.ROAD));
        assertThat(descriptions.get(1).transportDescription.get(2).toSite.uuid, is("1234565789"));
        assertThat(descriptions.get(1).transportDescription.get(2).modeOfTransport, is(ModeOfTransport.ROAD));

        assertThat(descriptions.get(2).transportDescription.get(0).modeOfTransport, is(ModeOfTransport.WATER));
        assertThat(descriptions.get(2).transportDescription.get(0).toSite.uuid, is("987654321"));
        assertThat(descriptions.get(2).transportDescription.get(1).fromSite.uuid, is("987654321"));
        assertThat(descriptions.get(2).transportDescription.get(1).modeOfTransport, is(ModeOfTransport.ROAD));
        assertThat(descriptions.get(2).transportDescription.get(2).toSite.uuid, is("987654321"));
        assertThat(descriptions.get(2).transportDescription.get(2).modeOfTransport, is(ModeOfTransport.ROAD));
    }


    @Test
    public void onewayExport() {

        Seaport antwerp = seaport("111");

        Terminal woerth = terminal("1234565789", TEN, TEN);
        Terminal malu = terminal("987654321", TEN, ZERO);

        when(terminalServiceMock.getAllActive()).thenReturn(asList(woerth, malu));

        MainRunConnection antwerpWoerthWater = connection(antwerp, woerth, BARGE);
        MainRunConnection antwerpWoerthRail = connection(antwerp, woerth, RAIL);

        when(connectionServiceMock.getConnectionsForTerminal(new BigInteger("1234565789"))).thenReturn(asList(
                antwerpWoerthWater, antwerpWoerthRail));

        MainRunConnection antwerpMaluWater = connection(antwerp, malu, BARGE);

        when(connectionServiceMock.getConnectionsForTerminal(new BigInteger("987654321"))).thenReturn(singletonList(
                antwerpMaluWater));

        TransportTemplateDto.TransportSegment terminalAddress = new TransportTemplateDto.TransportSegment(
                new TransportTemplateDto.TransportSite(TERMINAL, null, null, null),
                new TransportTemplateDto.TransportSite(ADDRESS, null, ONE, TEN), EMPTY, true, null);
        TransportTemplateDto.TransportSegment addressTerminal = new TransportTemplateDto.TransportSegment(
                new TransportTemplateDto.TransportSite(ADDRESS, null, ONE, TEN),
                new TransportTemplateDto.TransportSite(TERMINAL, null, null, null), FULL, true, null);

        TransportTemplateDto.TransportSegment terminalAntwerp = new TransportTemplateDto.TransportSegment(
                new TransportTemplateDto.TransportSite(TERMINAL, null, null, null),
                new TransportTemplateDto.TransportSite(SEAPORT, "111", null, null), FULL, true, null);

        TransportTemplateDto template = new TransportTemplateDto(asList(terminalAddress, addressTerminal,
                    terminalAntwerp));

        List<TransportDescriptionDto> descriptions = sut.from(template);

        assertThat(descriptions, hasSize(3));

        assertThat(descriptions.get(0).transportDescription.get(0).modeOfTransport, is(ModeOfTransport.ROAD));
        assertThat(descriptions.get(0).transportDescription.get(0).fromSite.uuid, is("1234565789"));
        assertThat(descriptions.get(0).transportDescription.get(1).toSite.uuid, is("1234565789"));
        assertThat(descriptions.get(0).transportDescription.get(1).modeOfTransport, is(ModeOfTransport.ROAD));
        assertThat(descriptions.get(0).transportDescription.get(2).fromSite.uuid, is("1234565789"));
        assertThat(descriptions.get(0).transportDescription.get(2).modeOfTransport, is(ModeOfTransport.WATER));

        assertThat(descriptions.get(1).transportDescription.get(0).modeOfTransport, is(ModeOfTransport.ROAD));
        assertThat(descriptions.get(1).transportDescription.get(0).fromSite.uuid, is("1234565789"));
        assertThat(descriptions.get(1).transportDescription.get(1).toSite.uuid, is("1234565789"));
        assertThat(descriptions.get(1).transportDescription.get(1).modeOfTransport, is(ModeOfTransport.ROAD));
        assertThat(descriptions.get(1).transportDescription.get(2).fromSite.uuid, is("1234565789"));
        assertThat(descriptions.get(1).transportDescription.get(2).modeOfTransport, is(ModeOfTransport.RAIL));

        assertThat(descriptions.get(2).transportDescription.get(0).modeOfTransport, is(ModeOfTransport.ROAD));
        assertThat(descriptions.get(2).transportDescription.get(0).fromSite.uuid, is("987654321"));
        assertThat(descriptions.get(2).transportDescription.get(1).toSite.uuid, is("987654321"));
        assertThat(descriptions.get(2).transportDescription.get(1).modeOfTransport, is(ModeOfTransport.ROAD));
        assertThat(descriptions.get(2).transportDescription.get(2).fromSite.uuid, is("987654321"));
        assertThat(descriptions.get(2).transportDescription.get(2).modeOfTransport, is(ModeOfTransport.WATER));
    }


    @Test
    public void roundtripExportDifferentSeaports() {

        Seaport antwerp = seaport("111");
        Seaport hamburg = seaport("112");

        Terminal woerth = terminal("1234565789", TEN, TEN);
        Terminal malu = terminal("987654321", TEN, ZERO);

        when(terminalServiceMock.getAllActive()).thenReturn(asList(woerth, malu));

        MainRunConnection antwerpWoerthWater = connection(antwerp, woerth, BARGE);
        MainRunConnection antwerpWoerthRail = connection(antwerp, woerth, RAIL);
        MainRunConnection hamburgWoerthRail = connection(hamburg, woerth, RAIL);

        when(connectionServiceMock.getConnectionsForTerminal(new BigInteger("1234565789"))).thenReturn(asList(
                antwerpWoerthWater, antwerpWoerthRail, hamburgWoerthRail));

        TransportTemplateDto.TransportSegment hamburgTerminal = new TransportTemplateDto.TransportSegment(
                new TransportTemplateDto.TransportSite(SEAPORT, "112", null, null),
                new TransportTemplateDto.TransportSite(TERMINAL, null, null, null), EMPTY, true, null);
        TransportTemplateDto.TransportSegment terminalAddress = new TransportTemplateDto.TransportSegment(
                new TransportTemplateDto.TransportSite(TERMINAL, null, null, null),
                new TransportTemplateDto.TransportSite(ADDRESS, null, ONE, TEN), EMPTY, true, null);
        TransportTemplateDto.TransportSegment addressTerminal = new TransportTemplateDto.TransportSegment(
                new TransportTemplateDto.TransportSite(ADDRESS, null, ONE, TEN),
                new TransportTemplateDto.TransportSite(TERMINAL, null, null, null), FULL, true, null);
        TransportTemplateDto.TransportSegment terminalAntwerp = new TransportTemplateDto.TransportSegment(
                new TransportTemplateDto.TransportSite(TERMINAL, null, null, null),
                new TransportTemplateDto.TransportSite(SEAPORT, "111", null, null), FULL, true, null);

        TransportTemplateDto template = new TransportTemplateDto(asList(hamburgTerminal, terminalAddress,
                    addressTerminal, terminalAntwerp));

        List<TransportDescriptionDto> descriptions = sut.from(template);

        assertThat(descriptions, hasSize(2));

        assertThat(descriptions.get(0).transportDescription.get(0).modeOfTransport, is(ModeOfTransport.RAIL));
        assertThat(descriptions.get(0).transportDescription.get(0).toSite.uuid, is("1234565789"));
        assertThat(descriptions.get(0).transportDescription.get(1).modeOfTransport, is(ModeOfTransport.ROAD));
        assertThat(descriptions.get(0).transportDescription.get(1).fromSite.uuid, is("1234565789"));
        assertThat(descriptions.get(0).transportDescription.get(2).toSite.uuid, is("1234565789"));
        assertThat(descriptions.get(0).transportDescription.get(2).modeOfTransport, is(ModeOfTransport.ROAD));
        assertThat(descriptions.get(0).transportDescription.get(3).fromSite.uuid, is("1234565789"));
        assertThat(descriptions.get(0).transportDescription.get(3).modeOfTransport, is(ModeOfTransport.WATER));

        assertThat(descriptions.get(1).transportDescription.get(0).modeOfTransport, is(ModeOfTransport.RAIL));
        assertThat(descriptions.get(1).transportDescription.get(0).toSite.uuid, is("1234565789"));
        assertThat(descriptions.get(1).transportDescription.get(1).modeOfTransport, is(ModeOfTransport.ROAD));
        assertThat(descriptions.get(1).transportDescription.get(1).fromSite.uuid, is("1234565789"));
        assertThat(descriptions.get(1).transportDescription.get(2).toSite.uuid, is("1234565789"));
        assertThat(descriptions.get(1).transportDescription.get(2).modeOfTransport, is(ModeOfTransport.ROAD));
        assertThat(descriptions.get(1).transportDescription.get(3).fromSite.uuid, is("1234565789"));
        assertThat(descriptions.get(1).transportDescription.get(3).modeOfTransport, is(ModeOfTransport.RAIL));
    }


    private Terminal terminal(String uuid, BigDecimal latitude, BigDecimal longitude) {

        Terminal woerth = new Terminal(new GeoLocation(latitude, longitude));
        woerth.setUniqueId(new BigInteger(uuid));

        return woerth;
    }


    private MainRunConnection connection(Seaport seaport, Terminal terminal, RouteType type) {

        MainRunConnection antwerpWoerthWater = new MainRunConnection(seaport);
        antwerpWoerthWater.setTerminal(terminal);
        antwerpWoerthWater.setRouteType(type);

        return antwerpWoerthWater;
    }


    private Seaport seaport(String uuid) {

        Seaport antwerp = new Seaport();
        antwerp.setUniqueId(new BigInteger(uuid));

        return antwerp;
    }
}
