package org.nuxeo.ecm.automation.core.test.directory;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.core.operations.users.GetNuxeoPrincipal;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = DefaultRepositoryInit.class)
@Deploy({ "org.nuxeo.ecm.directory.api", "org.nuxeo.ecm.directory",
        "org.nuxeo.ecm.directory.sql", "org.nuxeo.ecm.directory.types.contrib",
        "org.nuxeo.ecm.platform.usermanager.api",
        "org.nuxeo.ecm.platform.usermanager", "org.nuxeo.ecm.actions",
        "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.automation.features" })
@LocalDeploy("org.nuxeo.ecm.automation.features:test-user-directories-contrib.xml")
public class GetNuxeoUserTest {

    @Inject
    AutomationService service;

    @Inject
    CoreSession session;

    @Test
    public void shouldRetrieveCurrentPrincipalAsDoc() throws Exception {

        OperationContext ctx = new OperationContext(session);

        // test without params

        OperationChain chain = new OperationChain("fakeChain");
        chain.add(GetNuxeoPrincipal.ID);

        DocumentModel doc = (DocumentModel) service.run(ctx, chain);

        Assert.assertEquals("Jacky", doc.getPropertyValue("user:firstName"));
        Assert.assertEquals("Chan", doc.getPropertyValue("user:lastName"));
        Assert.assertEquals("Nuxeo", doc.getPropertyValue("user:company"));
        Assert.assertEquals("Administrator@example.com",
                doc.getPropertyValue("user:email"));

    }

    @Test
    public void shouldRetrievePrincipalAsDoc() throws Exception {

        OperationContext ctx = new OperationContext(session);

        // test with params

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("login", "jdoe");

        OperationChain chain = new OperationChain("fakeChain");
        OperationParameters oparams = new OperationParameters(
                GetNuxeoPrincipal.ID, params);
        chain.add(oparams);

        DocumentModel doc = (DocumentModel) service.run(ctx, chain);

        Assert.assertEquals("John", doc.getPropertyValue("user:firstName"));
        Assert.assertEquals("Doe", doc.getPropertyValue("user:lastName"));
        Assert.assertEquals("Nuxeo", doc.getPropertyValue("user:company"));
        Assert.assertEquals("jdoe@nuxeo.com",
                doc.getPropertyValue("user:email"));

    }
}
