package de.otto.edison.jobs.controller;

import de.otto.edison.jobs.domain.JobInfo;
import de.otto.edison.jobs.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.otto.edison.jobs.controller.JobRepresentation.representationOf;
import static java.net.URI.create;
import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

@RestController
public class JobsController {

    private static final Logger LOG = LoggerFactory.getLogger(JobsController.class);
    public static final int JOB_VIEW_COUNT = 100;

    @Autowired
    private JobRepository repository;
    @Value("${server.contextPath}")
    private String serverContextPath;

    public JobsController() {
    }

    JobsController(final JobRepository repository) {
        this.repository = repository;
    }

    @RequestMapping(value = "/jobs", method = RequestMethod.GET, produces = "text/html")
    public ModelAndView findJobsAsHtml(@RequestParam(value = "type", required = false) String type) {
        final List<JobRepresentation> jobRepresentations = findJobsAsJson(type, JOB_VIEW_COUNT);
        final ModelAndView modelAndView = new ModelAndView("jobs");
        modelAndView.addObject("jobs", jobRepresentations);
        return modelAndView;
    }

    @RequestMapping(value = "/jobs", method = RequestMethod.GET, produces = "application/json")
    public List<JobRepresentation> findJobsAsJson(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "count", required = false, defaultValue = "1") Integer count) {
        final Stream<JobInfo> filteredJobs;
        if (type == null) {
            filteredJobs = repository.findLatest(count).stream();
        } else {
            filteredJobs = repository.findLatestBy(type, count).stream();
        }

        return filteredJobs
                .map(JobRepresentation::representationOf)
                .collect(Collectors.toList());
    }

    @RequestMapping(value = "/jobs/{id}", method = RequestMethod.GET, produces = "text/html")
    public ModelAndView findJobAsHtml(final HttpServletRequest request,
                                      final HttpServletResponse response) throws IOException {
        final URI uri = create(request.getRequestURI());

        final Optional<JobInfo> optionalJob = repository.findBy(uri);
        if (optionalJob.isPresent()) {
            final ModelAndView modelAndView = new ModelAndView("job");
            modelAndView.addObject("job", representationOf(optionalJob.get()));
            return modelAndView;
        } else {
            response.sendError(SC_NOT_FOUND, "Job not found");
            return null;
        }
    }

    @RequestMapping(value = "/jobs/{id}", method = RequestMethod.GET, produces = "application/json")
    public JobRepresentation findJob(final HttpServletRequest request,
                                     final HttpServletResponse response) throws IOException {

        final URI uri = create(request.getRequestURI());

        final Optional<JobInfo> optionalJob = repository.findBy(uri);
        if (optionalJob.isPresent()) {
            return representationOf(optionalJob.get());
        } else {
            response.sendError(SC_NOT_FOUND, "Job not found");
            return null;
        }
    }

}
