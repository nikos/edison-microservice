package de.otto.edison.togglz.repository.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.otto.edison.mongo.AbstractMongoRepository;
import de.otto.edison.togglz.FeatureClassProvider;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.togglz.core.Feature;
import org.togglz.core.repository.FeatureState;
import org.togglz.core.repository.StateRepository;

import java.util.Map;
import java.util.Optional;

@Repository
public class MongoFeatureRepository extends AbstractMongoRepository<String, FeatureState> implements StateRepository {

    private static final Logger LOG = LoggerFactory.getLogger(MongoFeatureRepository.class);

    private static final String NAME = "_id";
    private static final String ENABLED = "enabled";
    private static final String STRATEGY = "strategy";
    private static final String PARAMETERS = "parameters";

    private final MongoCollection<Document> collection;
    private final FeatureClassProvider featureClassProvider;

    @Autowired
    public MongoFeatureRepository(final MongoDatabase database,
                                  final FeatureClassProvider featureClassProvider) {
        this.featureClassProvider = featureClassProvider;
        this.collection = database.getCollection("menuitemconfiguration");
    }

    @Override
    public FeatureState getFeatureState(final Feature feature) {
        Optional<FeatureState> featureState = findOne(feature.name());
        if (!featureState.isPresent()) {
            return null;
        }

        return featureState.get();
    }

    @Override
    public void setFeatureState(final FeatureState featureState) {
        createOrUpdate(featureState);
        LOG.info("switched feature toggle '{}' to '{}'", featureState.getFeature().name(), featureState.isEnabled());
    }

    @Override
    protected MongoCollection<Document> collection() {
        return collection;
    }

    @Override
    protected String keyOf(final FeatureState value) {
        return value.getFeature().name();
    }

    @Override
    protected Document encode(final FeatureState value) {
        Document document = new Document();

        document.append(NAME, value.getFeature().name());
        document.append(ENABLED, value.isEnabled());
        document.append(STRATEGY, value.getStrategyId());
        document.append(PARAMETERS, value.getParameterMap());

        return document;
    }

    @Override
    protected FeatureState decode(final Document document) {
        final String name = document.getString(NAME);
        final Boolean enabled = document.getBoolean(ENABLED);
        final String strategy = document.getString(STRATEGY);
        final Map<String, String> parameters = document.get(PARAMETERS, Map.class);

        final FeatureState featureState = new FeatureState(resolveEnumValue(name));
        featureState.setEnabled(enabled);
        featureState.setStrategyId(strategy);
        for (Map.Entry<String, String> parameter : parameters.entrySet()) {
            featureState.setParameter(parameter.getKey(), parameter.getValue());
        }

        return featureState;
    }

    @Override
    protected void ensureIndexes() {
        // no indices
    }

    private Feature resolveEnumValue(String name) {
        final Class enumType = featureClassProvider.getFeatureClass();
        return (Feature) Enum.valueOf(enumType, name);
    }
}
