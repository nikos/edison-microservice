package de.otto.edison.jobs.status;

import de.otto.edison.jobs.domain.JobInfo;
import de.otto.edison.jobs.repository.JobRepository;
import de.otto.edison.status.domain.StatusDetail;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

import static com.google.common.collect.ImmutableList.of;
import static de.otto.edison.jobs.status.JobStatusDetailIndicator.ERROR_MESSAGE;
import static de.otto.edison.jobs.status.JobStatusDetailIndicator.SUCCESS_MESSAGE;
import static de.otto.edison.status.domain.Status.OK;
import static de.otto.edison.status.domain.Status.WARNING;
import static java.time.Duration.ofHours;
import static java.time.OffsetDateTime.now;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Test
public class JobStatusDetailIndicatorTest {

    private JobRepository jobRepository;

    @BeforeMethod
    public void setUp() throws Exception {
        jobRepository = mock(JobRepository.class);
    }

    @Test
    public void shouldIndicateOkIfJobRunWasSuccessful() {
        // given
        OffsetDateTime now = now();

        JobInfo someJob = mock(JobInfo.class);
        when(someJob.getJobType()).thenReturn("someJobType");
        when(someJob.getJobUri()).thenReturn(URI.create("/some/job/url"));
        when(someJob.getStarted()).thenReturn(now.minusSeconds(1));
        when(someJob.getStopped()).thenReturn(Optional.of(now));
        when(someJob.getStatus()).thenReturn(JobInfo.JobStatus.OK);
        when(jobRepository.findLatestBy(anyString(), eq(1))).thenReturn(of(someJob));

        JobStatusDetailIndicator jobStatusDetailIndicator = new JobStatusDetailIndicator(jobRepository, "someName", "someJobType", "someBaseUrl", ofHours(10));

        // when
        StatusDetail status = jobStatusDetailIndicator.statusDetail();

        // then
        assertThat(status.getStatus(), is(OK));
    }

    @Test
    public void shouldIndicateWarningIfLastJobRunWasTooLongAgo() {
        // given
        OffsetDateTime now = now();

        JobInfo someJob = mock(JobInfo.class);
        when(someJob.getJobType()).thenReturn("someJobType");
        when(someJob.getJobUri()).thenReturn(URI.create("/some/job/url"));
        when(someJob.getStarted()).thenReturn(now.minusSeconds(21));
        when(someJob.getStopped()).thenReturn(Optional.of(now.minusSeconds(20)));
        when(someJob.getStatus()).thenReturn(JobInfo.JobStatus.OK);
        when(jobRepository.findLatestBy(anyString(), eq(1))).thenReturn(of(someJob));

        JobStatusDetailIndicator jobStatusDetailIndicator = new JobStatusDetailIndicator(jobRepository, "someName", "someJobType", "someBaseUrl", Duration.ofSeconds(10));

        // when
        StatusDetail status = jobStatusDetailIndicator.statusDetail();

        // then
        assertThat(status.getStatus(), is(WARNING));
        assertThat(status.getMessage(), containsString("Job didn't run in the past"));
    }

    @Test
    public void shouldHaveOkMessage() {
        // given
        OffsetDateTime now = now();

        JobInfo someJob = mock(JobInfo.class);
        when(someJob.getJobType()).thenReturn("someJobType");
        when(someJob.getJobUri()).thenReturn(URI.create("/some/job/url"));
        when(someJob.getStarted()).thenReturn(now.minusSeconds(1));
        when(someJob.getStopped()).thenReturn(Optional.empty());
        when(someJob.getStatus()).thenReturn(JobInfo.JobStatus.OK);
        when(jobRepository.findLatestBy(anyString(), eq(1))).thenReturn(of(someJob));

        JobStatusDetailIndicator jobStatusDetailIndicator = new JobStatusDetailIndicator(jobRepository, "someName", "someJobType", "someBaseUrl", ofHours(10));

        // when
        StatusDetail status = jobStatusDetailIndicator.statusDetail();

        // then
        assertThat(status.getMessage(), is(SUCCESS_MESSAGE));
    }

    @Test
    public void shouldHaveUri() {
        // given
        OffsetDateTime now = now();

        JobInfo someJob = mock(JobInfo.class);
        when(someJob.getJobUri()).thenReturn(URI.create("/some/uri"));
        when(someJob.getJobType()).thenReturn("someJobType");
        when(someJob.getStarted()).thenReturn(now.minusSeconds(1));
        when(someJob.getStopped()).thenReturn(Optional.empty());
        when(someJob.getStatus()).thenReturn(JobInfo.JobStatus.OK);
        when(jobRepository.findLatestBy(anyString(), eq(1))).thenReturn(of(someJob));

        JobStatusDetailIndicator jobStatusDetailIndicator = new JobStatusDetailIndicator(jobRepository, "someName", "someJobType", "someBaseUrl", ofHours(10));

        // when
        StatusDetail status = jobStatusDetailIndicator.statusDetail();

        // then
        assertThat(status.getDetails(), hasEntry("uri", "someBaseUrl/some/uri"));
    }

    @Test
    public void shouldHaveUriWhenNoJobIsRunning() {
        // given
        OffsetDateTime now = now();

        JobInfo someJob = mock(JobInfo.class);
        when(someJob.getJobType()).thenReturn("someJobType");
        when(someJob.getJobUri()).thenReturn(URI.create("/some/uri"));
        when(someJob.getStarted()).thenReturn(now.minusSeconds(1));
        when(someJob.getStopped()).thenReturn(Optional.of(now));
        when(someJob.getStatus()).thenReturn(JobInfo.JobStatus.OK);
        when(jobRepository.findLatestBy(anyString(), eq(1))).thenReturn(of(someJob));

        JobStatusDetailIndicator jobStatusDetailIndicator = new JobStatusDetailIndicator(jobRepository, "someName", "someJobType", "someBaseUrl", ofHours(10));

        // when
        StatusDetail status = jobStatusDetailIndicator.statusDetail();

        // then
        assertThat(status.getDetails(), hasEntry("uri", "someBaseUrl/some/uri"));
    }

    @Test
    public void shouldNotHaveUriOrRunningIfNoJobPresent() {
        // given
        when(jobRepository.findLatestBy(anyString(), eq(1))).thenReturn(of());

        JobStatusDetailIndicator jobStatusDetailIndicator = new JobStatusDetailIndicator(jobRepository, "someName", "someJobType", "someBaseUrl", ofHours(10));

        // when
        StatusDetail status = jobStatusDetailIndicator.statusDetail();

        // then
        assertThat(status.getDetails(), not(hasKey("uri")));
        assertThat(status.getDetails(), not(hasKey("running")));
    }

    @Test
    public void shouldIndicateThatJobIsRunning() {
        // given
        OffsetDateTime now = now();

        JobInfo someJob = mock(JobInfo.class);
        when(someJob.getJobType()).thenReturn("someJobType");
        when(someJob.getJobUri()).thenReturn(URI.create("/some/uri"));
        when(someJob.getStarted()).thenReturn(now.minusSeconds(1));
        when(someJob.getStopped()).thenReturn(Optional.empty());
        when(someJob.getStatus()).thenReturn(JobInfo.JobStatus.OK);
        when(jobRepository.findLatestBy(anyString(), eq(1))).thenReturn(of(someJob));

        JobStatusDetailIndicator jobStatusDetailIndicator = new JobStatusDetailIndicator(jobRepository, "someName", "someJobType", "someBaseUrl", ofHours(10));

        // when
        StatusDetail status = jobStatusDetailIndicator.statusDetail();

        // then
        assertThat(status.getDetails(), hasEntry("running", "someBaseUrl/some/uri"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldIndicateThatJobIsNotRunning() {
        // given
        OffsetDateTime now = now();

        JobInfo someJob = mock(JobInfo.class);
        when(someJob.getJobType()).thenReturn("someJobType");
        when(someJob.getJobUri()).thenReturn(URI.create("/some/job/url"));
        when(someJob.getStarted()).thenReturn(now.minusSeconds(1));
        when(someJob.getStopped()).thenReturn(Optional.of(now));
        when(someJob.getStatus()).thenReturn(JobInfo.JobStatus.OK);
        when(jobRepository.findLatestBy(anyString(), eq(1))).thenReturn(of(someJob));

        JobStatusDetailIndicator jobStatusDetailIndicator = new JobStatusDetailIndicator(jobRepository, "someName", "someJobType", "someBaseUrl", ofHours(10));

        // when
        StatusDetail status = jobStatusDetailIndicator.statusDetail();

        // then
        assertThat(status.getDetails(), not(hasKey("running")));
    }

    @Test
    public void shouldHaveErrorMessage() {
        // given
        OffsetDateTime now = now();

        JobInfo someJob = mock(JobInfo.class);
        when(someJob.getJobType()).thenReturn("someJobType");
        when(someJob.getJobUri()).thenReturn(URI.create("/some/job/url"));
        when(someJob.getStarted()).thenReturn(now.minusSeconds(1));
        when(someJob.getStopped()).thenReturn(Optional.empty());
        when(someJob.getStatus()).thenReturn(JobInfo.JobStatus.ERROR);
        when(jobRepository.findLatestBy(anyString(), eq(1))).thenReturn(of(someJob));

        JobStatusDetailIndicator jobStatusDetailIndicator = new JobStatusDetailIndicator(jobRepository, "someName", "someJobType", "someBaseUrl", ofHours(10));

        // when
        StatusDetail status = jobStatusDetailIndicator.statusDetail();

        // then
        assertThat(status.getMessage(), is(ERROR_MESSAGE));
    }

    @Test
    public void shouldIndicateWarningIfJobRunWasErrornous() {
        // given
        OffsetDateTime now = now();

        JobInfo someJob = mock(JobInfo.class);
        when(someJob.getJobType()).thenReturn("someJobType");
        when(someJob.getJobUri()).thenReturn(URI.create("/some/job/url"));
        when(someJob.getStarted()).thenReturn(now.minusSeconds(1));
        when(someJob.getStopped()).thenReturn(Optional.empty());
        when(someJob.getStatus()).thenReturn(JobInfo.JobStatus.ERROR);
        when(jobRepository.findLatestBy(anyString(), eq(1))).thenReturn(of(someJob));

        JobStatusDetailIndicator jobStatusDetailIndicator = new JobStatusDetailIndicator(jobRepository, "someName", "someJobType", "someBaseUrl", ofHours(10));

        // when
        StatusDetail status = jobStatusDetailIndicator.statusDetail();

        // then
        assertThat(status.getStatus(), is(WARNING));
    }

    @Test
    public void shouldIndicateWarningIfJobRunWasDead() {
        // given
        OffsetDateTime now = now();

        JobInfo someJob = mock(JobInfo.class);
        when(someJob.getJobType()).thenReturn("someJobType");
        when(someJob.getJobUri()).thenReturn(URI.create("/some/job/url"));
        when(someJob.getStarted()).thenReturn(now.minusSeconds(1));
        when(someJob.getStopped()).thenReturn(Optional.empty());
        when(someJob.getStatus()).thenReturn(JobInfo.JobStatus.DEAD);
        when(jobRepository.findLatestBy(anyString(), eq(1))).thenReturn(of(someJob));

        JobStatusDetailIndicator jobStatusDetailIndicator = new JobStatusDetailIndicator(jobRepository, "someName", "someJobType", "someBaseUrl", ofHours(10));

        // when
        StatusDetail status = jobStatusDetailIndicator.statusDetail();

        // then
        assertThat(status.getStatus(), is(WARNING));
    }

    @Test
    public void shouldIndicateWarningIfLastJobRunWasDead() {
        // given
        OffsetDateTime now = now();

        JobInfo someJob = mock(JobInfo.class);
        when(someJob.getJobType()).thenReturn("someJobType");
        when(someJob.getJobUri()).thenReturn(URI.create("/some/job/url"));
        when(someJob.getStarted()).thenReturn(now);
        when(someJob.getStopped()).thenReturn(Optional.empty());
        when(someJob.getStatus()).thenReturn(JobInfo.JobStatus.DEAD);
        when(jobRepository.findLatestBy(anyString(), eq(1))).thenReturn(of(someJob));

        JobStatusDetailIndicator jobStatusDetailIndicator = new JobStatusDetailIndicator(jobRepository, "someName", "someJobType", "someBaseUrl", ofHours(10));

        // when
        StatusDetail status = jobStatusDetailIndicator.statusDetail();

        // then
        assertThat(status.getStatus(), is(WARNING));
    }

    @Test
    public void shouldFilterByJobType() {
        // given
        OffsetDateTime now = now();

        JobInfo someJob = mock(JobInfo.class);
        when(someJob.getJobType()).thenReturn("someJobType");
        when(someJob.getJobUri()).thenReturn(URI.create("/some/job/url"));
        when(someJob.getStarted()).thenReturn(now.minusSeconds(1));
        when(someJob.getStopped()).thenReturn(Optional.empty());
        when(someJob.getStatus()).thenReturn(JobInfo.JobStatus.ERROR);
        when(jobRepository.findLatestBy(anyString(), eq(1))).thenReturn(of(someJob));

        JobStatusDetailIndicator jobStatusDetailIndicator = new JobStatusDetailIndicator(jobRepository, "someName", "someJobType2", "someBaseUrl", ofHours(10));

        // when
        StatusDetail status = jobStatusDetailIndicator.statusDetail();

        // then
        assertThat(status.getStatus(), is(WARNING));
    }

    @Test
    public void shouldAcceptIfNoJobRan() {
        // given
        JobStatusDetailIndicator jobStatusDetailIndicator = new JobStatusDetailIndicator(jobRepository, "someName", "someJobType", "someBaseUrl", ofHours(10));

        // when
        StatusDetail status = jobStatusDetailIndicator.statusDetail();

        // then
        assertThat(status.getStatus(), is(OK));
    }

    @Test
    public void shouldHaveName() {
        // given
        JobStatusDetailIndicator jobStatusDetailIndicator = new JobStatusDetailIndicator(jobRepository, "someName", "someJobType", "someBaseUrl", ofHours(10));

        // when
        StatusDetail status = jobStatusDetailIndicator.statusDetail();

        // then
        assertThat(status.getName(), is("someName"));
    }
}