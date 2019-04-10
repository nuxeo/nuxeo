/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.multi.tenant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.multi.tenant.Constants.TENANTS_DIRECTORY;
import static org.nuxeo.ecm.multi.tenant.Constants.TENANT_CONFIG_FACET;
import static org.nuxeo.ecm.multi.tenant.Constants.TENANT_ID_PROPERTY;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.6
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@RepositoryConfig
@Deploy({ "org.nuxeo.ecm.multi.tenant", "org.nuxeo.ecm.platform.login", "org.nuxeo.ecm.platform.web.common" })
@LocalDeploy({ "org.nuxeo.ecm.platform.test:test-usermanagerimpl/userservice-config.xml",
        "org.nuxeo.ecm.multi.tenant:multi-tenant-test-contrib.xml",
        "org.nuxeo.ecm.multi.tenant:multi-tenant-enabled-default-test-contrib.xml" })
public class TestTenantIsolationByDefault {

    @Inject
    protected CoreSession session;

    @Inject
    protected MultiTenantService multiTenantService;

    @Inject
    protected DirectoryService directoryService;

    @Test
    public void testTenantIsolationByDefault() throws ClientException {
        assertTrue(multiTenantService.isTenantIsolationEnabled(session));
        DocumentModel domain = session.getDocument(new PathRef("/default-domain"));
        assertNotNull(domain);
        assertTrue(domain.hasFacet(TENANT_CONFIG_FACET));
        assertEquals("default-domain", domain.getPropertyValue(TENANT_ID_PROPERTY));

        ACP acp = domain.getACP();
        ACL acl = acp.getOrCreateACL();
        assertNotNull(acl);

        Session session = null;
        try {
            session = directoryService.open(TENANTS_DIRECTORY);
            DocumentModelList docs = session.getEntries();
            assertEquals(1, docs.size());
            DocumentModel doc = docs.get(0);
            assertEquals(domain.getName(), doc.getPropertyValue("tenant:id"));
            assertEquals(domain.getTitle(), doc.getPropertyValue("tenant:label"));
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

}
