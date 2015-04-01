package de.otto.edison.jobs.repository;


import de.otto.edison.jobs.domain.JobInfo;
import de.otto.edison.jobs.domain.JobType;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.Optional;

import static de.otto.edison.jobs.domain.JobInfo.JobStatus.ERROR;
import static de.otto.edison.jobs.domain.JobInfo.JobStatus.OK;
import static de.otto.edison.jobs.domain.JobInfoBuilder.jobInfoBuilder;
import static de.otto.edison.testsupport.matcher.OptionalMatchers.isAbsent;
import static de.otto.edison.testsupport.matcher.OptionalMatchers.isPresent;
import static java.time.OffsetDateTime.now;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;

public class KeepLastJobsTest {


    @Test
    public void shouldRemoveJobsWithMatchingJobType() {
        // given
        JobType type = () -> "TYPE2";
        KeepLastJobs strategy = new KeepLastJobs(1, Optional.of(type));
        JobRepository repository = new InMemJobRepository() {{
            createOrUpdate(jobInfoBuilder(() -> "TYPE1", URI.create("foo")).withStopped(now()).build());
            createOrUpdate(jobInfoBuilder(type, URI.create("foobar")).withStopped(now()).build());
            createOrUpdate(jobInfoBuilder(type, URI.create("bar")).withStopped(now()).build());
        }};
        // when
        strategy.doCleanUp(repository);
        // then
        assertThat(repository.size(), is(2));
        assertThat(repository.findBy(type), hasSize(1));
    }

    @Test
    public void shouldRemoveOldestJobs() {
        // given
        JobType type = () -> "TYPE";
        KeepLastJobs strategy = new KeepLastJobs(2, Optional.of(type));
        JobRepository repository = new InMemJobRepository() {{
            createOrUpdate(jobInfoBuilder(type, URI.create("foo")).withStarted(now()).withStopped(now()).build());
            createOrUpdate(jobInfoBuilder(type, URI.create("foobar")).withStarted(now().minusSeconds(2)).withStopped(now()).build());
            createOrUpdate(jobInfoBuilder(type, URI.create("bar")).withStarted(now().minusSeconds(1)).withStopped(now()).build());
        }};
        // when
        strategy.doCleanUp(repository);
        // then
        assertThat(repository.size(), is(2));
        assertThat(repository.findBy(URI.create("foobar")), isAbsent());
    }

    @Test
    public void shouldOnlyRemoveStoppedJobs() {
        // given
        JobType type = () -> "TYPE";
        KeepLastJobs strategy = new KeepLastJobs(2, Optional.of(type));
        JobRepository repository = new InMemJobRepository() {{
            createOrUpdate(jobInfoBuilder(type, URI.create("one")).withStarted(now().minusSeconds(4)).build());
            createOrUpdate(jobInfoBuilder(type, URI.create("two")).withStarted(now().minusSeconds(3)).build());
            createOrUpdate(jobInfoBuilder(type, URI.create("three")).withStarted(now().minusSeconds(2)).withStopped(now()).build());
            createOrUpdate(jobInfoBuilder(type, URI.create("four")).withStarted(now().minusSeconds(1)).withStopped(now()).build());
            createOrUpdate(jobInfoBuilder(type, URI.create("five")).withStarted(now()).withStopped(now()).build());
        }};
        // when
        strategy.doCleanUp(repository);
        // then
        assertThat(repository.size(), is(2));
        assertThat(repository.findBy(URI.create("one")), isPresent());
        assertThat(repository.findBy(URI.create("two")), isPresent());
        assertThat(repository.findBy(URI.create("three")), isAbsent());
        assertThat(repository.findBy(URI.create("four")), isAbsent());
        assertThat(repository.findBy(URI.create("five")), isAbsent());
    }

    @Test
    public void shouldKeepAllRunningJobs() {
        // given
        JobType type = () -> "TYPE";
        KeepLastJobs strategy = new KeepLastJobs(2, Optional.of(type));
        JobRepository repository = new InMemJobRepository() {{
            createOrUpdate(jobInfoBuilder(type, URI.create("one")).withStarted(now().minusSeconds(4)).build());
            createOrUpdate(jobInfoBuilder(type, URI.create("two")).withStarted(now().minusSeconds(3)).build());
            createOrUpdate(jobInfoBuilder(type, URI.create("three")).withStarted(now().minusSeconds(2)).build());
            createOrUpdate(jobInfoBuilder(type, URI.create("four")).withStarted(now().minusSeconds(1)).withStopped(now()).build());
            createOrUpdate(jobInfoBuilder(type, URI.create("five")).withStarted(now()).withStopped(now()).build());
        }};
        // when
        strategy.doCleanUp(repository);
        // then
        assertThat(repository.size(), is(3));
        assertThat(repository.findBy(URI.create("one")), isPresent());
        assertThat(repository.findBy(URI.create("two")), isPresent());
        assertThat(repository.findBy(URI.create("three")), isPresent());
        assertThat(repository.findBy(URI.create("four")), isAbsent());
        assertThat(repository.findBy(URI.create("five")), isAbsent());
    }

    @Test
    public void shouldOnlyRemoveStoppedJobsWithMatchingJobType() {
        // given
        JobType type = () -> "TYPE2";
        KeepLastJobs strategy = new KeepLastJobs(1, Optional.of(type));
        JobRepository repository = new InMemJobRepository() {{
            createOrUpdate(jobInfoBuilder(() -> "TYPE1", URI.create("foo")).withStopped(now()).build());
            createOrUpdate(jobInfoBuilder(type, URI.create("foobar")).build());
            createOrUpdate(jobInfoBuilder(type, URI.create("bar")).build());
        }};
        // when
        strategy.doCleanUp(repository);
        // then
        assertThat(repository.size(), is(3));
        assertThat(repository.findBy(type), hasSize(2));
    }

    @Test
    public void shouldKeepTwoJobInfos() {
        // given
        KeepLastJobs strategy = new KeepLastJobs(2, Optional.<JobType>empty());
        JobRepository repository = new InMemJobRepository() {{
            createOrUpdate(jobInfoBuilder(() -> "TYPE", URI.create("foo")).withStopped(now()).build());
            createOrUpdate(jobInfoBuilder(() -> "TYPE", URI.create("foobar")).withStopped(now()).build());
            createOrUpdate(jobInfoBuilder(() -> "TYPE", URI.create("bar")).withStopped(now()).build());
        }};
        // when
        strategy.doCleanUp(repository);
        // then
        assertThat(repository.size(), is(2));
    }

    @Test
    public void shouldBeOkToKeepAllJobs() {
        // given
        KeepLastJobs strategy = new KeepLastJobs(2, Optional.<JobType>empty());
        JobRepository repository = new InMemJobRepository() {{
            createOrUpdate(jobInfoBuilder(() -> "TYPE", URI.create("bar")).withStopped(now()).build());
        }};
        // when
        strategy.doCleanUp(repository);
        // then
        assertThat(repository.size(), is(1));
    }

}