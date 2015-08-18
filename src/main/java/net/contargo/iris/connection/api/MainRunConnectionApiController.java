package net.contargo.iris.connection.api;

import com.wordnik.swagger.annotations.ApiOperation;

import net.contargo.iris.api.AbstractController;
import net.contargo.iris.api.RestApiErrorDto;
import net.contargo.iris.connection.dto.MainRunConnectionDto;
import net.contargo.iris.connection.dto.MainRunConnectionDtoService;
import net.contargo.iris.connection.dto.SimpleMainRunConnectionDto;
import net.contargo.iris.connection.service.DuplicateMainRunConnectionException;

import org.slf4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import org.springframework.stereotype.Controller;

import org.springframework.validation.Errors;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.lang.invoke.MethodHandles;

import java.math.BigInteger;

import java.net.URI;

import java.util.Collection;

import javax.validation.Valid;

import static org.slf4j.LoggerFactory.getLogger;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;


/**
 * Public API controller that responds to request for {@link net.contargo.iris.connection.MainRunConnection}s.
 *
 * @author  Sandra Thieme - thieme@synyx.de
 * @author  Oliver Messner - messner@synyx.de
 * @author  Tobias Schneider - schneider@synyx.de
 */
@Controller
@RequestMapping(value = "/connections")
public class MainRunConnectionApiController extends AbstractController {

    private static final Logger LOG = getLogger(MethodHandles.lookup().lookupClass());

    private final MainRunConnectionDtoService connectionApiDtoService;

    @Autowired
    public MainRunConnectionApiController(MainRunConnectionDtoService connectionApiDtoService) {

        this.connectionApiDtoService = connectionApiDtoService;
    }

    @ApiOperation(
        value = "Returns all Connections containing the given terminal",
        notes = "Returns all Connections containing the given terminal", response = SimpleMainRunConnectionDto.class,
        responseContainer = "List"
    )
    @RequestMapping(method = GET, params = "terminalUid", produces = "application/json")
    @ResponseBody
    public Collection<SimpleMainRunConnectionDto> getConnectionsForTerminal(
        @RequestParam("terminalUid") BigInteger terminalUid) {

        LOG.info("API: client requests connections for terminal UID: " + terminalUid);

        return connectionApiDtoService.getConnectionsForTerminal(terminalUid);
    }


    @RequestMapping(method = GET, value = "/{id}")
    public ResponseEntity<MainRunConnectionDto> getConnection(@PathVariable Long id) {

        MainRunConnectionDto dto = connectionApiDtoService.get(id);

        return new ResponseEntity<>(dto, OK);
    }


    @RequestMapping(method = POST, value = "")
    public ResponseEntity createConnection(@Valid @RequestBody MainRunConnectionDto dto, Errors errors) {

        if (errors.hasErrors()) {
            throw new ValidationException(errors);
        }

        MainRunConnectionDto savedDto = connectionApiDtoService.save(dto);

        URI location = ServletUriComponentsBuilder.fromCurrentServletMapping()
            .path("/../web/connections/{id}")
            .build()
            .expand(savedDto.getId())
            .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(location);

        return new ResponseEntity<>(headers, CREATED);
    }


    @RequestMapping(method = PUT, value = "/{id}")
    public ResponseEntity<MainRunConnectionDto> updateConnection(@Valid @RequestBody MainRunConnectionDto dto,
        Errors errors) {

        if (errors.hasErrors()) {
            throw new ValidationException(errors);
        }

        MainRunConnectionDto updatedDto = connectionApiDtoService.save(dto);

        return new ResponseEntity<>(updatedDto, OK);
    }


    @ExceptionHandler(DuplicateMainRunConnectionException.class)
    ResponseEntity<RestApiErrorDto> handleDuplicateMainRunConnectionException(DuplicateMainRunConnectionException e) {

        return new ResponseEntity<>(new RestApiErrorDto(e.getErrorCode(), e.getMessage()), BAD_REQUEST);
    }


    @ExceptionHandler(ValidationException.class)
    ResponseEntity<RestApiErrorDto> handleValidationException(ValidationException e) {

        return new ResponseEntity<>(new RestApiErrorDto("",
                    e.getErrors().getFieldError().getField() + ": "
                    + e.getErrors().getFieldError().getDefaultMessage()), BAD_REQUEST);
    }
}
