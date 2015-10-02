package org.nuxeo.elasticsearch.http.readonly;

import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.elasticsearch.ElasticSearchConstants;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.http.readonly.filter.AuditRequestFilter;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * @since 7.4
 */
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@LocalDeploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
@Deploy({ "org.nuxeo.elasticsearch.http.readonly",
        "org.nuxeo.elasticsearch.audit.test:elasticsearch-audit-index-test-contrib.xml" })
public class TestAuditRequestFilter {

    @Inject
    ElasticSearchAdmin esa;

    @Test
    public void testMatchAllAuditAsAdmin() throws Exception {
        String payload = "{\"query\": {\"match_all\": {}}}";
        AuditRequestFilter filter = new AuditRequestFilter();
        filter.init(TestSearchRequestFilter.getAdminCoreSession(),
                esa.getIndexNameForType(ElasticSearchConstants.ENTRY_TYPE), ElasticSearchConstants.ENTRY_TYPE,
                "pretty", payload);
        Assert.assertEquals(payload, filter.getPayload());
    }

    @Test
    public void testMatchAllAudigtAsNonAdmin() throws Exception {
        String payload = "{\"query\": {\"match_all\": {}}}";
        AuditRequestFilter filter = new AuditRequestFilter();
        try {
            filter.init(TestSearchRequestFilter.getNonAdminCoreSession(),
                    esa.getIndexNameForType(ElasticSearchConstants.ENTRY_TYPE), ElasticSearchConstants.ENTRY_TYPE,
                    "pretty", payload);
        } catch (IllegalArgumentException e) {
            // Expected
            return;
        }
        fail("Non Admin should not be able to access audit");
    }

}
