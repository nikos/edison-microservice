package de.otto.edison.jobs.service;

import de.otto.edison.jobs.domain.JobInfo;
import de.otto.edison.jobs.domain.JobMessage;
import de.otto.edison.jobs.repository.InMemJobRepository;
import de.otto.edison.jobs.repository.JobRepository;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static de.otto.edison.testsupport.matcher.OptionalMatchers.isPresent;
import static java.time.Clock.fixed;
import static java.time.ZoneId.systemDefault;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class DefaultJobServiceTest {

    private ScheduledExecutorService executorService;

    @BeforeMethod
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        this.executorService = mock(ScheduledExecutorService.class);
        doAnswer(new RunImmediately()).when(executorService).execute(any(Runnable.class));

        when(executorService.scheduleAtFixedRate(any(Runnable.class),anyLong(),anyLong(),any(TimeUnit.class))).thenReturn(mock(ScheduledFuture.class));
    }

    @Test
    public void shouldReturnCreatedJobUri() {
        // given:
        final Clock clock = fixed(Instant.now(), systemDefault());
        final DefaultJobService jobService = new DefaultJobService("/foo", new InMemJobRepository(), null, mock(GaugeService.class), clock, executorService);
        final JobRunnable jobRunnable = mock(JobRunnable.class);
        when(jobRunnable.getJobType()).thenReturn("BAR");
        // when:
        final URI jobUri = jobService.startAsyncJob(jobRunnable);
        // then:
        assertThat(jobUri.toString(), startsWith("/foo/internal/jobs/"));
    }

    @Test
    public void shouldPersistJobs() {
        // given:
        final Clock clock = fixed(Instant.now(), systemDefault());
        final InMemJobRepository jobRepository = new InMemJobRepository();
        final DefaultJobService jobService = new DefaultJobService("/foo", jobRepository, null, mock(GaugeService.class), clock, executorService);
        final JobRunnable jobRunnable = mock(JobRunnable.class);
        when(jobRunnable.getJobType()).thenReturn("BAR");
        // when:
        final URI jobUri = jobService.startAsyncJob(jobRunnable);
        // then:
        assertThat(jobRepository.findBy(jobUri), isPresent());
    }

    @Test
    public void shouldRunJobs() {
        // given:
        final Clock clock = fixed(Instant.now(), systemDefault());
        final InMemJobRepository jobRepository = new InMemJobRepository();
        final JobRunnable jobRunnable = mock(JobRunnable.class);
        when(jobRunnable.getJobType()).thenReturn("BAR");
        final DefaultJobService jobService = new DefaultJobService("/foo", jobRepository, null, mock(GaugeService.class), clock, executorService);
        // when:
        final URI jobUri = jobService.startAsyncJob(jobRunnable);
        // then:
        final JobInfo jobInfo = jobRepository.findBy(jobUri).get();
        assertThat(jobInfo.getStopped(), isPresent());
    }

    @Test
    public void shouldRunJobsByType() {
        // given:
        final Clock clock = fixed(Instant.now(), systemDefault());
        final InMemJobRepository jobRepository = new InMemJobRepository();
        final JobRunnable jobRunnable = mock(JobRunnable.class);
        when(jobRunnable.getJobType()).thenReturn("BAR");
        final DefaultJobService jobService = new DefaultJobService("/foo", jobRepository, asList(jobRunnable), mock(GaugeService.class), clock, executorService);
        // when:
        final URI jobUri = jobService.startAsyncJob("bar");
        // then:
        final JobInfo jobInfo = jobRepository.findBy(jobUri).get();
        assertThat(jobInfo.getStopped(), isPresent());
    }

    @Test
    public void shouldNotRunSameJobsInParallel() {
        // given:
        final Clock clock = fixed(Instant.now(), systemDefault());
        final InMemJobRepository jobRepository = new InMemJobRepository();
        final JobRunnable jobRunnable = mock(JobRunnable.class);
        when(jobRunnable.getJobType()).thenReturn("BAR");
        URI alreadyRunningJob = URI.create("/internal/jobs/barIsRunning");
        jobRepository.createOrUpdate(new JobInfo("BAR", alreadyRunningJob, OffsetDateTime.now(), Optional.<OffsetDateTime>empty(), Collections.<JobMessage>emptyList(), JobInfo.JobStatus.OK, OffsetDateTime.now()));
        final DefaultJobService jobService = new DefaultJobService("/foo", jobRepository, asList(jobRunnable), mock(GaugeService.class), clock, executorService);
        // when:
        final URI jobUri = jobService.startAsyncJob("bar");
        // then:
        assertThat(jobUri, is(alreadyRunningJob));
    }

    @Test
    public void shouldRunDifferentJobsInParallel() {
        // given:
        final Clock clock = fixed(Instant.now(), systemDefault());
        final InMemJobRepository jobRepository = new InMemJobRepository();
        final JobRunnable jobRunnable = mock(JobRunnable.class);
        when(jobRunnable.getJobType()).thenReturn("FOO");
        URI alreadyRunningJob = URI.create("/internal/jobs/barIsRunning");
        jobRepository.createOrUpdate(new JobInfo("BAR", alreadyRunningJob, OffsetDateTime.now(), Optional.<OffsetDateTime>empty(), Collections.<JobMessage>emptyList(), JobInfo.JobStatus.OK, OffsetDateTime.now()));
        final DefaultJobService jobService = new DefaultJobService("/foo", jobRepository, asList(jobRunnable), mock(GaugeService.class), clock, executorService);
        // when:
        final URI jobUri = jobService.startAsyncJob("foo");
        // then:
        assertThat(jobUri, is(not(alreadyRunningJob)));
    }

    @Test
    public void shouldReportRuntime() {
        // given:
        final Clock clock = fixed(Instant.now(), systemDefault());

        final JobRunnable jobRunnable = mock(JobRunnable.class);
        when(jobRunnable.getJobType()).thenReturn("BAR");

        final GaugeService mock = mock(GaugeService.class);
        final DefaultJobService jobService = new DefaultJobService("/foo", mock(JobRepository.class), null, mock, clock, executorService);
        // when:
        jobService.startAsyncJob(jobRunnable);
        // then:
        verify(mock).submit(eq("gauge.jobs.runtime.bar"), anyLong());
    }

    private static class RunImmediately implements Answer {
        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            Runnable runnable = (Runnable) invocation.getArguments()[0];
            runnable.run();
            return null;
        }
    }
}