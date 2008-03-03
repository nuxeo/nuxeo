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

package org.nuxeo.ecm.platform.search.core.listener;

import java.io.Serializable;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;
import org.nuxeo.ecm.core.search.NXSearch;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedData;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResource;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.factory.BuiltinDocumentFields;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.query.ComposedNXQuery;
import org.nuxeo.ecm.core.search.api.client.query.impl.ComposedNXQueryImpl;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultItem;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.document.ResourceType;
import org.nuxeo.runtime.api.Framework;

/**
 * Test factory.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestListener extends RepositoryTestCase {

    private static final Log log = LogFactory.getLog(TestListener.class);

    private static final String ENGINE_NAME = "compass";

    protected CoreSession remote;

    private SearchService service;

    private final IndexingDocumentModelPrefetchedListener listener = new IndexingDocumentModelPrefetchedListener();

    private final Random random = new Random(new Date().getTime());

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deploy("ServiceManagement.xml");
        deploy("LoginComponent.xml");
        deploy("RepositoryManager.xml");

        deploy("EventService.xml");

        deploy("CoreService.xml");
        deploy("TypeService.xml");
        deploy("PolicyService.xml");
        deploy("SecurityService.xml");
        deploy("RepositoryService.xml");
        deploy("test-CoreExtensions.xml");
        deploy("CoreTestExtensions.xml");
        deploy("DemoRepository.xml");
        deploy("LifeCycleService.xml");
        deploy("LifeCycleServiceExtensions.xml");
        deploy("CoreEventListenerService.xml");
        deploy("PlatformService.xml");
        deploy("DefaultPlatform.xml");

        deploy("nxmimetype-bundle.xml");
        deploy("nxtransform-framework.xml");
        deploy("nxtransform-platform-contrib.xml");
        deploy("nxtransform-plugins-bundle.xml");

        deploy("nxsearch-test-framework.xml");
        deploy("nxsearch-test-contrib.xml");

        deploy("nxsearch-" + ENGINE_NAME + "-test-contrib.xml");

        service = NXSearch.getSearchService();
        assertNotNull(service);

        RepositoryManager mgr = Framework.getService(RepositoryManager.class);
        remote = mgr.getDefaultRepository().open();
        assertNotNull(remote);

        DocumentModelImpl documentModelImpl = new DocumentModelImpl("User");
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("accessLevel", Long.valueOf(3));
        documentModelImpl.addDataModel(new DataModelImpl("user", data));
        ((NuxeoPrincipal)remote.getPrincipal()).setModel(documentModelImpl);
    }

    private String generateUnique() {
        return String.valueOf(random.nextLong());
    }

    private DocumentModel createChildDocument(DocumentModel childFolder)
            throws ClientException {

        DocumentModel ret = remote.createDocument(childFolder);

        assertNotNull(ret);
        assertNotNull(ret.getName());
        assertNotNull(ret.getId());
        assertNotNull(ret.getRef());
        assertNotNull(ret.getPathAsString());

        return ret;
    }

    private DocumentModel createSampleFile() throws Exception {

        // Create a document model.
        DocumentModel root = remote.getRootDocument();
        DocumentModel dm = new DocumentModelImpl(root.getPathAsString(),
                "file#" + generateUnique(), "File");
        dm = createChildDocument(dm);
        remote.save();

        assertEquals("project", remote.getCurrentLifeCycleState(dm.getRef()));
        assertEquals("File", dm.getType());

        dm.setProperty("dublincore", "title", "Indexable data");
        dm.setProperty("dublincore", "description", "Indexable description");
        dm.setProperty("file", "filename", "foo.pdf");
        String[] contributors = new String[] { "a", "b" };
        dm.setProperty("dublincore", "contributors", contributors);

        // add a blob
        StringBlob sb = new StringBlob("<doc>Indexing baby</doc>");
        byte[] bytes = sb.getByteArray();
        Blob blob = new ByteArrayBlob(bytes, "text/html", "ISO-8859-15");
        dm.setProperty("file", "content", blob);

        dm.setProperty("dublincore", "created", new Date());

        dm = remote.saveDocument(dm);
        remote.save();

        // remote.disconnect();
        return dm;
    }

    public void testResolvedResourceGeneration() throws Exception {
        DocumentModel dm = createSampleFile();
        dm = remote.getDocument(dm.getRef());
        assertNotNull(dm);

        // populate models
        Map<String, Serializable> prefetchInfo = new HashMap<String, Serializable>();
        prefetchInfo.put("common.icon", "icon");
        prefetchInfo.put("dc.title", "title");
        prefetchInfo.put("dc.modified", new GregorianCalendar());
        prefetchInfo.put("dc.contributors", new String[0]);
        prefetchInfo.put("sp.securityLevel", Long.valueOf(3));
        if (prefetchInfo != null) {
            for (String k : prefetchInfo.keySet()) {
                dm.prefetchProperty(k, prefetchInfo.get(k));
            }
        }

        assertFalse(dm.getPrefetch().isEmpty());

        ResolvedResources resources = IndexingDocumentModelPrefetchedListener.computePrefetchedResourcesFor(dm, dm.getPrefetch());
        assertEquals(3, resources.getIndexableResolvedResources().size());

        ResolvedResource dc = resources.getIndexableResolvedResourceByConfName("dublincore");
        assertNotNull(dc);
        assertEquals(dm.getId(), dc.getId());

        IndexableResourceConf conf = dc.getConfiguration();
        assertNotNull(conf);
        assertEquals("dublincore", conf.getName());
        assertEquals("dc", conf.getPrefix());
        assertEquals(ResourceType.SCHEMA, conf.getType());

        assertNotNull(dc.getIndexableResource());
        assertEquals(3, dc.getIndexableData().size());

        ResolvedData data;

        data = dc.getIndexableDataByName("modified");
        assertTrue(data.getValue() instanceof GregorianCalendar);
        assertTrue(data.isSortable());

        data = dc.getIndexableDataByName("title");
        assertNotNull(data);
        assertTrue(data.isSortable());
        assertEquals("text", data.getTypeName().toLowerCase());
        assertTrue(data.isStored());

        ResolvedResource common = resources.getIndexableResolvedResourceByConfName("common");
        assertNotNull(common);
        assertEquals(common.getId(), common.getId());

        conf = common.getConfiguration();
        assertEquals("common", conf.getName());
        assertEquals("common", conf.getPrefix());
        assertEquals(ResourceType.SCHEMA, conf.getType());

        assertEquals(1, common.getIndexableData().size());
        assertNotNull(common.getIndexableResource());

        service.index(resources);

        ComposedNXQuery query = new ComposedNXQueryImpl(
                "SELECT * FROM Document WHERE dc:title LIKE 'title'");
        query.setSearchPrincipal(service.getSearchPrincipal(remote.getPrincipal()));
        ResultSet res = service.searchQuery(query, 0, 10);
        assertEquals(1, res.getTotalHits());
        assertEquals(1, res.size());
        ResultItem item = res.get(0);
        assertEquals(dm.getId(), item.getName());
        assertEquals("title", item.get("dc:title"));
        assertEquals("icon", item.get("common:icon"));
        assertTrue(item.get("dc:modified") instanceof GregorianCalendar);
        assertNotNull(item.get("dc:contributors"));
        assertEquals("project", item.get(BuiltinDocumentFields.FIELD_DOC_LIFE_CYCLE));


        // Test Security Policy
        assertEquals("3", item.get("sp:securityLevel"));

        ((NuxeoPrincipal)remote.getPrincipal()).getModel().setProperty("user", "accessLevel", Long.valueOf(2));

        query.setSearchPrincipal(service.getSearchPrincipal(remote.getPrincipal()));
        res = service.searchQuery(query, 0, 10);
        assertEquals(0, res.getTotalHits());
        assertEquals(0, res.size());
    }

}
