/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id:TestSearchEnginePluginRegistration.java 13121 2007-03-01 18:07:58Z janguenot $
 */

package org.nuxeo.ecm.platform.audit.search;

import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.factory.IndexableResourceFactory;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.ResourceTypeDescriptor;
import org.nuxeo.ecm.platform.audit.search.resources.indexing.api.ResourceType;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * Test search engine plugins registration.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestAuditIndexableResource extends NXRuntimeTestCase {

    private SearchService service;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployContrib("org.nuxeo.ecm.platform.audit.search.tests",
                "nxsearch-test-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.audit.search.tests",
                "nxsearch-test-contrib.xml");

        // Local lookup is enough
        service = Framework.getLocalService(SearchService.class);
        assertNotNull(service);
    }

    public void testAuditResourceConfRegistration() {
        IndexableResourceConf conf = service.getIndexableResourceConfByName("audit", false);
        assertNotNull(conf);

        // FIXME => here we need to fix search service side for this.
        //conf = service.getIndexableResourceConfByName("audit", true);
        //assertNotNull(conf);
    }

    public void testAuditResourceTypeRegistration() {
        ResourceTypeDescriptor desc = service.getResourceTypeDescriptorByName(ResourceType.AUDIT);
        assertNotNull(desc);
        IndexableResourceFactory factory = desc.getFactory();
        assertNotNull(factory.createEmptyIndexableResource());
    }

}
