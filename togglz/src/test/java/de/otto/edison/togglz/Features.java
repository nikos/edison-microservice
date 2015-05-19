package de.otto.edison.togglz;

import org.togglz.core.Feature;
import org.togglz.core.annotation.Label;
import org.togglz.core.context.FeatureContext;

public enum Features implements Feature {

    @Label("test")
    TEST;

    public boolean isActive() {
        return FeatureContext.getFeatureManager().isActive(this);
    }
}

