package org.nuxeo.ecm.permissions;

import java.net.URL;

import org.nuxeo.common.xmap.XMap;
import org.nuxeo.ecm.core.api.security.impl.UserEntryImpl;
import org.nuxeo.ecm.webapp.security.SecurityDataPolicy;
import org.nuxeo.ecm.webapp.security.SecurityDataPolicyDescriptor;
import org.nuxeo.ecm.webapp.security.policies.LeafWeightComparator;
import org.nuxeo.ecm.webapp.security.policies.LeafWeightSecurityDataPolicy;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestSecurityPolicy extends NXRuntimeTestCase {


    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.event");
        deployBundle("org.nuxeo.ecm.core.query");
        deployBundle("org.nuxeo.ecm.platform");
        deployBundle("org.nuxeo.ecm.platform.api");
        deployBundle("org.nuxeo.ecm.platform.comment.core");
        deployBundle("org.nuxeo.ecm.platform.comment.api");
        deployBundle("org.nuxeo.ecm.platform.types.core");
        deployBundle("org.nuxeo.ecm.webapp.core");
        deployBundle("org.nuxeo.ecm.webapp.core.tests");
    }

    public void testPolicyMapping() throws Exception {
        XMap xmap = new XMap();
        xmap.register(SecurityDataPolicyDescriptor.class);
        URL url = Thread.currentThread().getContextClassLoader()
            .getResource("leaf-security-policy.xml");
        SecurityDataPolicyDescriptor desc = (SecurityDataPolicyDescriptor) xmap.load(url);
        assertNotNull(desc);
        assertTrue(desc.clazz.isAssignableFrom(LeafWeightSecurityDataPolicy.class));;
    }

    public void testDeployed() {
        SecurityDataPolicy policy = Framework.getLocalService(SecurityDataPolicy.class);
        assertNotNull(policy);
    }

    public void testComparator() {
        UserEntryImpl e1 = new UserEntryImpl("pfff");
        e1.addPrivilege("Read", true, false);
        UserEntryImpl e2 = new UserEntryImpl("pfff");
        e2.addPrivilege("Comment", false, false);

        assertTrue("e1 should comes before e2", new LeafWeightComparator().compare(e1, e2) > 0);

    }

}
