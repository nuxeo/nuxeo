/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.test.NuxeoLoginFeature;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.6
 */
@RunWith(FeaturesRunner.class)
@Features({ NuxeoLoginFeature.class, PlatformFeature.class })
@RepositoryConfig
@Deploy("org.nuxeo.ecm.multi.tenant")
@Deploy("org.nuxeo.ecm.platform.userworkspace")
@Deploy("org.nuxeo.ecm.core.cache")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.default.config")
@Deploy("org.nuxeo.ecm.platform.test:test-usermanagerimpl/userservice-config.xml")
@Deploy("org.nuxeo.ecm.multi.tenant:multi-tenant-test-contrib.xml")
@Deploy("org.nuxeo.ecm.multi.tenant:multi-tenant-enabled-default-test-contrib.xml")
public class TestTenantIsolationByDefault {

    @Inject
    protected CoreSession session;

    @Inject
    protected MultiTenantService multiTenantService;

    @Inject
    protected DirectoryService directoryService;

    @Test
    public void testTenantIsolationByDefault() {
        assertTrue(multiTenantService.isTenantIsolationEnabled(session));
        DocumentModel domain = session.getDocument(new PathRef("/default-domain"));
        assertNotNull(domain);
        assertTrue(domain.hasFacet(TENANT_CONFIG_FACET));
        assertEquals("default-domain", domain.getPropertyValue(TENANT_ID_PROPERTY));

        ACP acp = domain.getACP();
        ACL acl = acp.getOrCreateACL();
        assertNotNull(acl);

        try (Session session = directoryService.open(TENANTS_DIRECTORY)) {
            DocumentModelList docs = session.getEntries();
            assertEquals(1, docs.size());
            DocumentModel doc = docs.get(0);
            assertEquals(domain.getName(), doc.getPropertyValue("tenant:id"));
            assertEquals(domain.getTitle(), doc.getPropertyValue("tenant:label"));
        }
    }

}
