package net.contargo.iris.transport.inclinations.client.contargo;

import net.contargo.iris.transport.inclinations.client.ElevationProviderClient;
import net.contargo.iris.transport.inclinations.dto.Point2D;
import net.contargo.iris.transport.inclinations.dto.Point3D;

import org.springframework.web.client.RestTemplate;

import java.util.List;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;


/**
 * @author  Ben Antony - antony@synyx.de
 */
public class ElevationServiceClient implements ElevationProviderClient {

    private static final String URL = "{elevationServiceHost}/elevation";

    private final RestTemplate restTemplate;
    private final String elevationServiceHost;

    public ElevationServiceClient(RestTemplate restTemplate, String elevationServiceHost) {

        this.restTemplate = restTemplate;
        this.elevationServiceHost = elevationServiceHost;
    }

    @Override
    public List<Point3D> getElevations(List<Point2D> points) {

        return stream(restTemplate.postForObject(URL, toElevationServicePoints(points),
                        ElevationServicePoint3D[].class, elevationServiceHost)).map(p ->
                    new Point3D(p.getElevation(), p.getLatitude(), p.getLongitude())).collect(toList());
    }


    private List<ElevationServicePoint2D> toElevationServicePoints(List<Point2D> points) {

        return points.stream()
            .map(p -> new ElevationServicePoint2D(p.getLatitude(), p.getLongitude(), p.getOsmId()))
            .collect(toList());
    }
}
