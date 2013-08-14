package org.nuxeo.elasticsearch.test;

import javax.sql.DataSource;


import org.nuxeo.ecm.core.storage.sql.DatabaseDerby;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.core.storage.sql.ra.PoolingRepositoryFactory;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
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
