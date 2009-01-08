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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryTestCase;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.threading.IndexingThreadPool;
import org.nuxeo.runtime.api.Framework;

/**
 * Test factory.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public class TestIndexingThreadPool extends RepositoryTestCase {

    protected CoreSession remote;

    private final Random random = new Random(new Date().getTime());

    @Override
    public void setUp() throws Exception {
        super.setUp();

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
                "CoreEventListenerService.xml");
        deployContrib("org.nuxeo.ecm.platform.search.tests",
                "PlatformService.xml");
        deployContrib("org.nuxeo.ecm.platform.search.tests",
                "DefaultPlatform.xml");

        deployContrib("org.nuxeo.ecm.platform.search.tests",
                "nxmimetype-bundle.xml");
        deployContrib("org.nuxeo.ecm.platform.search.tests",
                "nxtransform-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.search.tests",
                "nxtransform-platform-contrib.xml");
        deployContrib("org.nuxeo.ecm.platform.search.tests",
                "nxtransform-plugins-bundle.xml");

        deployContrib("org.nuxeo.ecm.platform.search.tests",
                "nxsearch-test-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.search.tests",
                "nxsearch-test-contrib.xml");

        SearchService service = NXSearch.getSearchService();
        assertNotNull(service);

        RepositoryManager mgr = Framework.getService(RepositoryManager.class);
        remote = mgr.getDefaultRepository().open();

        assertNotNull(remote);
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

        dm.setProperty("dublincore", "created", Calendar.getInstance());

        dm = remote.saveDocument(dm);
        remote.save();

        // remote.disconnect();
        return dm;
    }

    public void testThreadPool() throws Exception {

        List<DocumentModel> dms = new ArrayList<DocumentModel>();
        for (int i = 0; i < 10; i++) {
            DocumentModel dm = createSampleFile();
            assertNotNull(dm);
            //assertEquals(0, IndexingThreadPool.getActiveIndexingTasks());
            IndexingThreadPool.index(dm, false);
            dms.add(dm);
        }

        while (IndexingThreadPool.getActiveIndexingTasks() > 0) {
            Thread.sleep(300);
        }

        //assertEquals(10,
        //        IndexingThreadPool.getTotalCompletedIndexingTasks());

        /*
         * for (String uuid : uuids) { String queryStr = "SELECT * FROM Document
         * WHERE " + BuiltinDocumentFields.FIELD_DOC_TYPE + " LIKE 'File'";
         * ResultSet set = service.searchQuery(new
         * ComposedNXQueryImpl(queryStr), 0, 10); assertEquals(10, set.size()); }
         */

        while (IndexingThreadPool.getActiveIndexingTasks() > 0) {
            Thread.sleep(300);
        }

        for (DocumentModel dm : dms) {
            //assertEquals(0, IndexingThreadPool.getActiveIndexingTasks());
            IndexingThreadPool.unindex(dm, false);
        }

        while (IndexingThreadPool.getActiveIndexingTasks() > 0) {
            Thread.sleep(300);
        }

        //assertEquals(20,
        //        IndexingThreadPool.getTotalCompletedIndexingTasks());
    }

}
