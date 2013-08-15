package org.nuxeo.elasticsearch.test;

import org.nuxeo.ecm.core.storage.sql.DatabaseDerby;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.SimpleFeature;

@Deploy({"org.nuxeo.elasticsearch.core"})
@Features({CoreFeature.class})
//@Features({CoreFeature.class, TransactionalFeature.class})
//@RepositoryConfig(cleanup=Granularity.METHOD, repositoryFactoryClass=PoolingRepositoryFactory.class)
public class RepositoryElasticSearchFeature extends SimpleFeature {

    @Override
    public void initialize(FeaturesRunner runner)
            throws Exception {
        DatabaseHelper.setDatabaseForTests(DatabaseDerby.class.getCanonicalName());
    }



}
