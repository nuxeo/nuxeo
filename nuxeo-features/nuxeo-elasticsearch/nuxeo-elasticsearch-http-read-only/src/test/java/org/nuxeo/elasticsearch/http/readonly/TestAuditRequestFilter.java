package org.nuxeo.elasticsearch.http.readonly;

import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.elasticsearch.audit.ESAuditBackend;
import org.nuxeo.elasticsearch.http.readonly.filter.AuditRequestFilter;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @since 7.4
 */
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@LocalDeploy("org.nuxeo.elasticsearch.core:elasticsearch-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.http.readonly")
public class TestAuditRequestFilter {

    private static final String INDICES = ESAuditBackend.IDX_NAME;

    private static final String TYPES = ESAuditBackend.IDX_TYPE;

    @Test
    public void testMatchAllAuditAsAdmin() throws Exception {
        String payload = "{\"query\": {\"match_all\": {}}}";
        AuditRequestFilter filter = new AuditRequestFilter();
        filter.init(TestSearchRequestFilter.getAdminCoreSession(), INDICES, TYPES, "pretty", payload);
        Assert.assertEquals(payload, filter.getPayload());
    }

    @Test
    public void testMatchAllAudigtAsNonAdmin() throws Exception {
        String payload = "{\"query\": {\"match_all\": {}}}";
        AuditRequestFilter filter = new AuditRequestFilter();
        try {
        filter.init(TestSearchRequestFilter.getNonAdminCoreSession(), INDICES, TYPES, "pretty", payload);
        } catch (IllegalArgumentException e) {
            //Expected
            return;
        }
        fail("Non Admin should not be able to access audit");
    }

}
