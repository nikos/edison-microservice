package de.otto.edison.togglz.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.togglz.core.context.StaticFeatureManagerProvider;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.manager.TogglzConfig;
import org.togglz.spring.manager.FeatureManagerFactory;

import javax.annotation.Resource;

@Configuration
public class FeatureManagerConfiguration {

    @Resource
    private TogglzConfig togglzConfig;

    @Bean
    public FeatureManager featureManager() throws Exception {
        FeatureManagerFactory featureManagerFactory = new FeatureManagerFactory();
        featureManagerFactory.setTogglzConfig(togglzConfig);
        FeatureManager featureManager = featureManagerFactory.getObject();
        StaticFeatureManagerProvider.setFeatureManager(featureManager);  // this workaround should be fixed with togglz version 2.2
        return featureManager;
    }
}
