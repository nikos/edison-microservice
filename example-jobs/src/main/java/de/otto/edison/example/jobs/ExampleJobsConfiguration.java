package de.otto.edison.example.jobs;

import de.otto.edison.jobs.definition.JobDefinition;
import de.otto.edison.jobs.repository.cleanup.KeepLastJobs;
import de.otto.edison.jobs.repository.cleanup.StopDeadJobs;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

import static de.otto.edison.jobs.definition.DefaultJobDefinition.fixedDelayJobDefinition;
import static java.time.Clock.systemDefaultZone;
import static java.time.Duration.ofHours;

/**
 * @author Guido Steinacker
 * @since 01.03.15
 */
@Configuration
public class ExampleJobsConfiguration {

    @Bean
    public KeepLastJobs keepLast10FooJobsCleanupStrategy() {
        return new KeepLastJobs(10, Optional.empty());
    }

    @Bean
    public StopDeadJobs stopDeadJobsStrategy() {
        return new StopDeadJobs(60, systemDefaultZone());
    }

    @Bean
    public JobDefinition fooJobDefinition() {
        return fixedDelayJobDefinition(
                "FooJob",
                "Foo Job",
                "An example job that is running for a while.",
                ofHours(1),
                Optional.of(ofHours(3))
        );
    }

    @Bean
    public JobDefinition barJobDefinition() {
        return fixedDelayJobDefinition(
                "BarJob",
                "Bar Job",
                "An example job that is running for a while.",
                ofHours(1),
                Optional.of(ofHours(3))
        );
    }
}
