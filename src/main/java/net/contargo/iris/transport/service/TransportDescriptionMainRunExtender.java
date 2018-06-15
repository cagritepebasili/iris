package net.contargo.iris.transport.service;

import net.contargo.iris.connection.MainRunConnection;
import net.contargo.iris.connection.service.MainRunConnectionService;
import net.contargo.iris.route.RouteType;
import net.contargo.iris.terminal.Region;
import net.contargo.iris.transport.api.ModeOfTransport;
import net.contargo.iris.transport.api.SiteType;
import net.contargo.iris.transport.api.TransportResponseDto;

import java.math.BigDecimal;
import java.math.BigInteger;

import static net.contargo.iris.transport.api.SiteType.SEAPORT;
import static net.contargo.iris.transport.api.SiteType.TERMINAL;
import static net.contargo.iris.transport.service.FlowDirection.DOWNSTREAM;
import static net.contargo.iris.transport.service.FlowDirection.UPSTREAM;

import static java.math.RoundingMode.UP;


/**
 * @author  Ben Antony - antony@synyx.de
 */
public class TransportDescriptionMainRunExtender {

    // average speeds in km/h
    private static final BigDecimal AVERAGE_SPEED_BARGE_UPSTREAM = new BigDecimal("10.0");
    private static final BigDecimal AVERAGE_SPEED_BARGE_DOWNSTREAM = new BigDecimal("18.0");
    private static final BigDecimal AVERAGE_SPEED_RAIL = new BigDecimal("45.0");
    private static final BigDecimal SECONDS_IN_AN_HOUR = new BigDecimal("60.0");

    private static final int DIGITS_TO_ROUND = 0;
    private static final int SCALE = 2;

    private final MainRunConnectionService mainRunConnectionService;

    public TransportDescriptionMainRunExtender(MainRunConnectionService mainRunConnectionService) {

        this.mainRunConnectionService = mainRunConnectionService;
    }

    void with(TransportResponseDto.TransportResponseSegment segment) {

        MainRunConnection mainRunConnection = getMainRunConnection(segment);
        ModeOfTransport modeOfTransport = segment.modeOfTransport;

        switch (modeOfTransport) {
            case RAIL:
                extendRailSegment(segment, mainRunConnection);
                break;

            case WATER:
                extendWaterSegment(segment, mainRunConnection);
                break;

            default:
                throw new IllegalArgumentException(modeOfTransport
                    + " is not a valid mode of transport for a main run segment");
        }
    }


    private MainRunConnection getMainRunConnection(TransportResponseDto.TransportResponseSegment segment) {

        BigInteger terminalUuid = getUuid(segment, TERMINAL);
        BigInteger seaportUuid = getUuid(segment, SEAPORT);

        RouteType routeType = segment.modeOfTransport.getRouteType();

        return mainRunConnectionService.getConnectionByTerminalUidAndSeaportUidAndType(terminalUuid, seaportUuid,
                routeType);
    }


    private static void extendRailSegment(TransportResponseDto.TransportResponseSegment segment,
        MainRunConnection mainRunConnection) {

        BigDecimal dieselDistance = mainRunConnection.getRailDieselDistance();
        BigDecimal electricDistance = mainRunConnection.getRailElectricDistance();

        BigDecimal railDistance = dieselDistance.add(electricDistance);

        segment.distance = railDistance.intValue();
        segment.duration = calculateDuration(railDistance, AVERAGE_SPEED_RAIL);
        segment.co2 = Co2Calculator.rail(dieselDistance.intValue(), electricDistance.intValue(), segment.loadingState);
    }


    private static void extendWaterSegment(TransportResponseDto.TransportResponseSegment segment,
        MainRunConnection mainRunConnection) {

        FlowDirection flowDirection = getFlowDirection(segment);
        BigDecimal divisor;

        switch (flowDirection) {
            case UPSTREAM:
                divisor = AVERAGE_SPEED_BARGE_UPSTREAM;
                break;

            case DOWNSTREAM:
                divisor = AVERAGE_SPEED_BARGE_DOWNSTREAM;
                break;

            default:
                throw new IllegalArgumentException("Unknown flow direction: " + flowDirection);
        }

        BigDecimal bargeDistance = mainRunConnection.getBargeDieselDistance().setScale(SCALE, UP);
        Region region = mainRunConnection.getTerminal().getRegion();

        segment.distance = bargeDistance.intValue();
        segment.duration = calculateDuration(bargeDistance, divisor);
        segment.co2 = Co2Calculator.water(segment.distance, region, segment.loadingState, flowDirection);
    }


    private static FlowDirection getFlowDirection(TransportResponseDto.TransportResponseSegment segment) {

        if (segment.fromSite.type == SEAPORT && segment.toSite.type == TERMINAL) {
            return UPSTREAM;
        }

        if (segment.fromSite.type == TERMINAL && segment.toSite.type == SEAPORT) {
            return DOWNSTREAM;
        }

        throw new IllegalArgumentException("Flow direction can not be determined");
    }


    static int calculateDuration(BigDecimal distance, BigDecimal divisor) {

        return distance.multiply(SECONDS_IN_AN_HOUR).divide(divisor, DIGITS_TO_ROUND, UP).intValue();
    }


    private static BigInteger getUuid(TransportResponseDto.TransportResponseSegment segment, SiteType siteType) {

        if (segment.fromSite.type == siteType) {
            return new BigInteger(segment.fromSite.uuid);
        } else if (segment.toSite.type == siteType) {
            return new BigInteger(segment.toSite.uuid);
        } else {
            return null;
        }
    }
}
