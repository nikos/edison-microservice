package de.otto.edison.jobs.repository;

import de.otto.edison.jobs.domain.JobInfo;
import de.otto.edison.jobs.repository.inmem.InMemJobRepository;
import org.testng.annotations.Test;

import java.time.Clock;

import static de.otto.edison.jobs.domain.JobInfo.newJobInfo;
import static de.otto.edison.testsupport.matcher.OptionalMatchers.isAbsent;
import static de.otto.edison.testsupport.matcher.OptionalMatchers.isPresent;
import static java.net.URI.create;
import static java.time.Clock.systemDefaultZone;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;

public class InMemJobInfoRepositoryTest {

    private Clock clock = systemDefaultZone();

    @Test
    public void shouldFindJobInfoByUri() {
        // given
        final InMemJobRepository repository = new InMemJobRepository();
        // when

        final JobInfo job = newJobInfo(create("/jobs/" + randomUUID()), "MYJOB", (j)-> {}, clock);
        repository.createOrUpdate(job);

        // then
        assertThat(repository.findOne(job.getJobUri()), isPresent());
    }

    @Test
    public void shouldReturnAbsentStatus() {
        InMemJobRepository repository = new InMemJobRepository();
        assertThat(repository.findOne(create("/foo/bar")), isAbsent());
    }

}
