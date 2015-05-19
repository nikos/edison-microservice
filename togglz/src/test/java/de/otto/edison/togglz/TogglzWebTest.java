package de.otto.edison.togglz;

import de.otto.edison.testsupport.applicationdriver.SpringTestBase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@Test
public class TogglzWebTest extends SpringTestBase {

    private final static RestTemplate restTemplate = new RestTemplate();

    @Test
    public void shouldRegisterTogglzConsole() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:8085/togglztest/internal/togglz/", String.class);
        assertThat(response.getStatusCode().is2xxSuccessful(), is(true));
    }

    @Test
    public void shouldAllowToggleStateToBeRetrievedInRequests() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:8085/togglztest/featurestate/test", String.class);
        assertThat(response.getStatusCode().is2xxSuccessful(), is(true));
        assertThat(response.getBody(),is("feature is active"));
    }
}