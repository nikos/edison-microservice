package de.otto.edison.status.indicator;

import de.otto.edison.status.domain.ApplicationStatus;
import de.otto.edison.status.domain.VersionInfo;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static de.otto.edison.status.domain.ApplicationStatus.applicationStatus;
import static java.net.InetAddress.getLocalHost;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

/**
 * @author Guido Steinacker
 * @since 13.02.15
 */
public class CachedApplicationStatusAggregator implements ApplicationStatusAggregator {

    private volatile ApplicationStatus cachedStatus;

    private final String applicationName;
    private final VersionInfo versionInfo;
    private final List<StatusDetailIndicator> indicators;
    private final String hostName;

    public CachedApplicationStatusAggregator(final String applicationName,
                                             final VersionInfo versionInfo,
                                             final List<StatusDetailIndicator> indicators) {
        this.applicationName = applicationName;
        this.versionInfo = versionInfo;
        this.indicators = unmodifiableList(new ArrayList<>(indicators));
        this.hostName = hostName();
        this.cachedStatus = calcApplicationStatus();
    }

    @Override
    public ApplicationStatus aggregatedStatus() {
        return cachedStatus;
    }

    @Override
    public void update() {
        System.out.println("updating...");
        this.cachedStatus = calcApplicationStatus();
    }

    private ApplicationStatus calcApplicationStatus() {
        return applicationStatus(applicationName, hostName, versionInfo, indicators
                        .stream()
                        .map(StatusDetailIndicator::statusDetail)
                        .collect(toList())
        );
    }

    private String hostName() {
        try {
            final String envHost = System.getenv("HOST");
            if (envHost != null) {
                return envHost;
            } else {
                return getLocalHost().getHostName();
            }
        } catch (final UnknownHostException e) {
            return "UNKOWN";
        }
    }

}
