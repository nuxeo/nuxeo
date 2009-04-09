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

package org.nuxeo.ecm.core.search;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.document.DocumentIndexableResource;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.document.impl.DocumentIndexableResourceImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Test document indexable resources.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestDocumentIndexableResources extends RepositoryTestCase {

    protected CoreSession remote;

    private SearchService service;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployContrib("org.nuxeo.ecm.platform.search.tests",
                "LoginComponent.xml");
        deployContrib("org.nuxeo.ecm.platform.search.tests",
                "RepositoryManager.xml");
        deployContrib("org.nuxeo.ecm.platform.search.tests",
                "CoreTestExtensions.xml");
        deployContrib("org.nuxeo.ecm.platform.search.tests",
                "DemoRepository.xml");
        deployContrib("org.nuxeo.ecm.platform.search.tests",
                "LifeCycleService.xml");
        deployContrib("org.nuxeo.ecm.platform.search.tests",
                "LifeCycleServiceExtensions.xml");
        deployContrib("org.nuxeo.ecm.platform.search.tests",
                "PlatformService.xml");
        deployContrib("org.nuxeo.ecm.platform.search.tests",
                "DefaultPlatform.xml");
        deployContrib("org.nuxeo.ecm.platform.search.tests",
                "nxsearch-test-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.search.tests",
                "nxsearch-test-contrib.xml");

        service = Framework.getLocalService(SearchService.class);
        assertNotNull(service);

        RepositoryManager mgr = Framework.getService(RepositoryManager.class);
        remote = mgr.getDefaultRepository().open();

        assertNotNull(remote);
    }

    public void testGetValue() throws Exception {

        // Create a document model.
        DocumentModel root = remote.getRootDocument();
        DocumentModel dm = new DocumentModelImpl(root.getPathAsString(),
                "File", "File");
        dm = remote.createDocument(dm);
        remote.save();

        dm.setProperty("dublincore", "title", "Indexable data");
        remote.saveDocument(dm);

        assertEquals("Indexable data", (String) dm.getProperty("dublincore",
                "title"));

        remote.disconnect();

        // :XXX: refactor test
        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        ctx.put("username", SecurityConstants.ADMINISTRATOR);
        remote = CoreInstance.getInstance().open("demo", ctx);

        dm = remote.getDocument(dm.getRef());

        assertEquals("Indexable data", (String) dm.getProperty("dublincore",
                "title"));

        // testing indexable resource that reuses the same CoreSession
        DocumentIndexableResource resource = new DocumentIndexableResourceImpl(dm, null, dm.getSessionId());

        assertEquals("Indexable data", resource.getValueFor("dublincore:title"));

        remote.disconnect();

        // testing indexable resource that uses it's own CoreSession
        resource = new DocumentIndexableResourceImpl(dm, null, null);

        assertEquals("Indexable data", resource.getValueFor("dublincore:title"));


        // id computation
        String resId = resource.computeId();
        assertNotNull(resId);
        // impl dependent, might change in the future
        assertTrue(resId.startsWith(dm.getRepositoryName()));

        resource.closeCoreSession();
    }

}
