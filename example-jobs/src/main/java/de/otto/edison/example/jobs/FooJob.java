package de.otto.edison.example.jobs;

import de.otto.edison.jobs.domain.Level;
import de.otto.edison.jobs.service.JobLogger;
import de.otto.edison.jobs.service.JobRunnable;
import org.springframework.stereotype.Component;

import java.util.Random;

import static de.otto.edison.jobs.domain.JobMessage.jobMessage;
import static java.lang.Thread.sleep;

/**
 * @author Guido Steinacker
 * @since 15.02.15
 */
@Component
public class FooJob implements JobRunnable {

    private FooJob() {
    }

    public static FooJob fooJob() {
        return new FooJob();
    }

    public String getJobType() {
        return "FOO";
    }

    @Override
    public void execute(final JobLogger jobLogger) {
        for (int i = 0; i < 10; ++i) {
            doSomeHardWork(jobLogger);
        }
    }

    private void doSomeHardWork(final JobLogger jobLogger) {
        try {
            jobLogger.log(jobMessage(Level.INFO, "Still doing some hard work..."));
            sleep(new Random(42).nextInt(2000));
        } catch (InterruptedException e) {
        /* ignore */
        }
    }

}
