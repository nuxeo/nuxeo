package org.nuxeo.ecm.automation.server.test;

import java.util.HashMap;

import org.codehaus.jackson.JsonNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.client.OperationRequest;
import org.nuxeo.ecm.automation.client.Session;
import org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient;
import org.nuxeo.ecm.automation.client.model.PathRef;
import org.nuxeo.ecm.automation.core.operations.services.AuditLog;
import org.nuxeo.ecm.automation.core.operations.services.AuditPageProviderOperation;
import org.nuxeo.ecm.automation.core.operations.services.DocumentPageProviderOperation;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.audit.AuditFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({EmbeddedAutomationServerFeature.class, AuditFeature.class})
@Jetty(port = 18080)
@RepositoryConfig(cleanup = Granularity.CLASS)
public class RemoteAuditTest {

    @Inject
    HttpAutomationClient client;

    @Test
    public void logAndThenQueryNoMapping() throws Exception {

        Session session = client.getSession("Administrator", "Administrator");
        Assert.assertNotNull(session);

        OperationRequest logRequest = session.newRequest(AuditLog.ID, new HashMap<String, Object>());

        logRequest.getParameters().put("event", "testing");
        logRequest.setInput(new PathRef("/"));
        logRequest.execute();

        OperationRequest  queryRequest = session.newRequest(AuditPageProviderOperation.ID, new HashMap<String, Object>());

        queryRequest.getParameters().put("providerName", "AUDIT_BROWSER");
        Object result = queryRequest.execute();
        JsonNode node = (JsonNode)result;

        //System.out.println(result.toString());
        int count = node.get("resultsCount").getValueAsInt();
        JsonNode entries = node.get("entries");
        System.out.println(entries);
        for (int i = 0 ; i < count; i++) {
            Assert.assertEquals("logEntry", entries.get(i).get("entity-type").getValueAsText());
            System.out.println(entries.get(i).toString());
        }

    }
}
