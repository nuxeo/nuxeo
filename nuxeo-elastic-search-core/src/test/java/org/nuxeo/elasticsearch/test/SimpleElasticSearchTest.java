package org.nuxeo.elasticsearch.test;

import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(BareElasticSearchFeature.class)
@Deploy({  })
public class SimpleElasticSearchTest {

    @Inject
    protected Node elasticSearchNode;

    @Inject
    protected Client elasticSearchClient;

    @Test
    public void shouldHaveStartedElasticSearch() throws Exception {
        Assert.assertNotNull(elasticSearchNode);
        Assert.assertNotNull(elasticSearchClient);
        Assert.assertFalse(elasticSearchNode.isClosed());
    }

}
