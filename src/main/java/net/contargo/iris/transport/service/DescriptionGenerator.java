package net.contargo.iris.transport.service;

import net.contargo.iris.connection.MainRunConnection;
import net.contargo.iris.connection.service.MainRunConnectionService;
import net.contargo.iris.terminal.Terminal;
import net.contargo.iris.terminal.service.TerminalService;
import net.contargo.iris.transport.api.ModeOfTransport;
import net.contargo.iris.transport.api.SiteType;
import net.contargo.iris.transport.api.TransportDescriptionDto;
import net.contargo.iris.transport.api.TransportTemplateDto;

import java.math.BigInteger;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static net.contargo.iris.transport.api.ModeOfTransport.RAIL;
import static net.contargo.iris.transport.api.ModeOfTransport.ROAD;
import static net.contargo.iris.transport.api.ModeOfTransport.WATER;
import static net.contargo.iris.transport.api.SiteType.SEAPORT;
import static net.contargo.iris.transport.api.SiteType.TERMINAL;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;


/**
 * @author  Ben Antony - antony@synyx.de
 * @author  Sandra Thieme - thieme@synyx.de
 */
public class DescriptionGenerator {

    private static final List<ModeOfTransport> ALL_MAIN_RUN_MODE_OF_TRANSPORTS = asList(WATER, RAIL);

    private final TerminalService terminalService;
    private final MainRunConnectionService mainRunConnectionService;

    public DescriptionGenerator(TerminalService terminalService, MainRunConnectionService mainRunConnectionService) {

        this.terminalService = terminalService;
        this.mainRunConnectionService = mainRunConnectionService;
    }

    /**
     * Generates all possible {@link net.contargo.iris.transport.api.TransportDescriptionDto}s for a given
     * {@link net.contargo.iris.transport.api.TransportTemplateDto}, where the terminal is unknown.
     *
     * @param  template  the template
     *
     * @return  a list of {@link net.contargo.iris.transport.api.TransportDescriptionDto}
     */
    public List<TransportDescriptionDto> from(TransportTemplateDto template) {

        Set<String> seaports = template.transportDescription.stream()
                .filter(DescriptionGenerator::containsSeaport)
                .map(DescriptionGenerator::getSeaportUuid)
                .collect(toSet());

        Map<Terminal, List<MainRunConnection>> terminalConnections = terminalService.getAllActive()
                .stream()
                .collect(toMap(Function.identity(), getTerminalConnectionsWithSeaports(seaports)));

        return terminalConnections.entrySet()
            .stream()
            .map(entry -> createDescriptionsForTerminal(template, entry.getKey(), entry.getValue()))
            .flatMap(List::stream)
            .collect(toList());
    }


    private static List<TransportDescriptionDto> createDescriptionsForTerminal(TransportTemplateDto template,
        Terminal terminal, List<MainRunConnection> connections) {

        TransportDescriptionDto initialDescription = new TransportDescriptionDto(template);
        IntermediateDescriptions descriptions = new IntermediateDescriptions(initialDescription);

        Map<BigInteger, List<ModeOfTransport>> seaportMots = extractSeaportsWithModeOfTransports(connections);

        template.transportDescription.stream()
            .filter(s -> isMainRunSegment(s.fromSite.type, s.toSite.type))
            .forEach(s -> {
                String seaportUuid = getSeaportUuid(s);

                List<ModeOfTransport> mots = seaportMots.getOrDefault(new BigInteger(seaportUuid), emptyList());

                List<TransportDescriptionDto> updatedDescriptions = augmentDescriptionsForEachModeOfTransport(mots,
                        terminal, seaportUuid, descriptions.get());

                descriptions.updateWith(updatedDescriptions);
            });

        return descriptions.get();
    }


    private static Map<BigInteger, List<ModeOfTransport>> extractSeaportsWithModeOfTransports(
        List<MainRunConnection> connections) {

        return connections.stream().collect(groupingBy(mainRunConnection ->
                        mainRunConnection.getSeaport()
                        .getUniqueId(),
                    mapping(mainRunConnection -> ModeOfTransport.fromRouteType(mainRunConnection.getRouteType()),
                        toList())));
    }


    private static List<TransportDescriptionDto> augmentDescriptionsForEachModeOfTransport(
        List<ModeOfTransport> modeOfTransports, Terminal terminal, String seaportUuid,
        List<TransportDescriptionDto> descriptions) {

        return modeOfTransports.stream()
            .map(m -> descriptions.stream().map(d -> generateResult(d, terminal, seaportUuid, m)).collect(toList()))
            .flatMap(List::stream)
            .collect(toList());
    }


    private static String getSeaportUuid(TransportTemplateDto.TransportSegment s) {

        if (s.fromSite.type == SEAPORT) {
            return s.fromSite.uuid;
        } else if (s.toSite.type == SEAPORT) {
            return s.toSite.uuid;
        } else {
            throw new IllegalArgumentException("Does not contain seaport");
        }
    }


    private static TransportDescriptionDto generateResult(TransportDescriptionDto original, Terminal terminal,
        String seaportUuid, ModeOfTransport mot) {

        TransportDescriptionDto descriptionDto = new TransportDescriptionDto(original);
        descriptionDto.transportDescription.forEach(s -> {
            if (s.fromSite.type == TERMINAL) {
                s.fromSite.uuid = terminal.getUniqueId().toString();
            }

            if (s.toSite.type == TERMINAL) {
                s.toSite.uuid = terminal.getUniqueId().toString();
            }

            if (isMainRunSegment(s.fromSite.type, s.toSite.type)) {
                if (segmentWithSeaport(s, seaportUuid)) {
                    s.modeOfTransport = mot;
                }
            } else {
                s.modeOfTransport = ROAD;
            }
        });

        return descriptionDto;
    }


    private static boolean segmentWithSeaport(TransportDescriptionDto.TransportSegment s, String seaportUuid) {

        if (s.fromSite.type == SEAPORT) {
            return s.fromSite.uuid.equals(seaportUuid);
        } else if (s.toSite.type == SEAPORT) {
            return s.toSite.uuid.equals(seaportUuid);
        } else {
            return false;
        }
    }


    private Function<Terminal, List<MainRunConnection>> getTerminalConnectionsWithSeaports(Set<String> seaports) {

        return
            t -> mainRunConnectionService.getConnectionsForTerminal(t.getUniqueId())
                .stream()
                .filter(MainRunConnection::getEnabled)
                .filter(DescriptionGenerator::matchingMot)
                .filter(matchingSeaports(seaports))
                .collect(toList());
    }


    private static Predicate<MainRunConnection> matchingSeaports(Set<String> seaports) {

        return c -> seaports.stream().anyMatch(s -> new BigInteger(s).equals(c.getSeaport().getUniqueId()));
    }


    private static boolean matchingMot(MainRunConnection c) {

        return ALL_MAIN_RUN_MODE_OF_TRANSPORTS.stream().map(ModeOfTransport::getRouteType).anyMatch(routeType ->
                    routeType == c.getRouteType());
    }


    private static boolean containsSeaport(TransportTemplateDto.TransportSegment segment) {

        return segment.fromSite.type == SEAPORT || segment.toSite.type == SEAPORT;
    }


    private static boolean isMainRunSegment(SiteType fromType, SiteType toType) {

        return (fromType == SEAPORT && toType == TERMINAL) || (fromType == TERMINAL && toType == SEAPORT);
    }
}
