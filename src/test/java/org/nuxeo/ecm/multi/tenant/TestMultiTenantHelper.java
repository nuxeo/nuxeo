package org.nuxeo.ecm.multi.tenant;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, PlatformFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.multi.tenant", "org.nuxeo.ecm.platform.login",
        "org.nuxeo.ecm.platform.web.common" })
@LocalDeploy("org.nuxeo.ecm.multi.tenant:multi-tenant-test-contrib.xml")
public class TestMultiTenantHelper {

    @Inject protected CoreSession session;

    @Inject protected UserManager userManager;

    /**
     * just to test it does not throw an exception.
     * 
     * @throws Exception
     */
    @Test public void testGetCurrentTenantId() throws Exception {
        MultiTenantHelper.getCurrentTenantId(new SystemPrincipal("nobody"));
    }

    /**
     * just to test it does not throw an exception.
     * 
     * @throws Exception
     */
    @Test public void testGetTenantId() throws Exception {
        MultiTenantHelper.getTenantId("nobody");
    }

}
