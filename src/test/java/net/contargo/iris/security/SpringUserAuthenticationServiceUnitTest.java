package net.contargo.iris.security;

import org.junit.Before;
import org.junit.Test;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.is;

import static java.util.Collections.singletonList;


/**
 * Unit test of {@link SpringUserAuthenticationService}.
 *
 * @author  David Schilling - schilling@synyx.de
 */
public class SpringUserAuthenticationServiceUnitTest {

    private SpringUserAuthenticationService sut;

    @Before
    public void setUp() {

        sut = new SpringUserAuthenticationService();
    }


    @Test
    public void getCurrentUser() {

        UserDetails userDetails = new User("user", "password", singletonList(new SimpleGrantedAuthority("USER")));
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
            new UsernamePasswordAuthenticationToken(userDetails, null);
        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);

        Authentication currentUser = sut.getCurrentUser();
        assertThat(currentUser.getPrincipal(), is(userDetails));
        assertThat(currentUser.getName(), is("user"));
    }
}