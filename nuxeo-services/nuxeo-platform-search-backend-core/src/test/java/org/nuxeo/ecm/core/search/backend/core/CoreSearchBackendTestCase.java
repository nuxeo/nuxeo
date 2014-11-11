/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.search.backend.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.query.impl.ComposedNXQueryImpl;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/*
 * @author Florent Guillaume
 */
public abstract class CoreSearchBackendTestCase extends NXRuntimeTestCase {

    public SearchService service;

    public CoreSession session;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.schema");
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core");
        deployBundle("org.nuxeo.ecm.core.event");
        deployRepository();
        deployBundle("org.nuxeo.ecm.platform.search");
        deployBundle("org.nuxeo.ecm.platform.search.backend.core");
        service = Framework.getService(SearchService.class);
        openSession();
    }

    @Override
    public void tearDown() throws Exception {
        undeployRepository();
        super.tearDown();
    }

    protected abstract void deployRepository() throws Exception;

    protected abstract void undeployRepository() throws Exception;

    public void openSession() throws ClientException {
        Map<String, Serializable> context = new HashMap<String, Serializable>();
        context.put("username", SecurityConstants.ADMINISTRATOR);
        session = CoreInstance.getInstance().open("test", context);
    }

    protected void makeDocuments() throws Exception {
        DocumentModel folder1 = new DocumentModelImpl("/", "testfolder1",
                "Folder");
        folder1.setPropertyValue("dc:title", "testfolder1_Title");
        folder1 = session.createDocument(folder1);

        DocumentModel file1 = new DocumentModelImpl("/testfolder1",
                "testfile1", "File");
        file1.setPropertyValue("dc:title", "testfile1_Title");
        file1.setPropertyValue("dc:description", "testfile1_description");
        String content = "This is a file.\nCaf\u00e9.";
        String filename = "testfile.txt";
        ByteArrayBlob blob1 = new ByteArrayBlob(content.getBytes("UTF-8"),
                "text/plain");
        file1.setPropertyValue("content", blob1);
        file1.setPropertyValue("filename", filename);
        file1 = session.createDocument(file1);

        DocumentModel file2 = new DocumentModelImpl("/testfolder1",
                "testfile2", "File");
        file2.setPropertyValue("dc:title", "testfile2_Title");
        file2.setPropertyValue("dc:description", "testfile2_DESCRIPTION2");
        file2 = session.createDocument(file2);

        session.save();
    }

    public void testInitialization() {
        assertNotNull(service);
        assertNotNull(session);
    }

    public void testSimpleQuery() throws Exception {
        makeDocuments();

        String sql = "SELECT * FROM Document";
        ResultSet results = service.searchQuery(new ComposedNXQueryImpl(sql),
                0, 100);
        assertEquals(3, results.size());
    }

    public void testReplay() throws Exception {
        makeDocuments();

        String sql = "SELECT * FROM File WHERE dc:title = 'testfile1_Title'";
        ResultSet results = service.searchQuery(new ComposedNXQueryImpl(sql),
                0, 100);
        assertEquals(1, results.getTotalHits());
        assertEquals(1, results.getPageHits());
        assertEquals(0, results.getOffset());
        assertEquals(100, results.getRange());
        assertFalse(results.hasNextPage());
        assertTrue(results.isFirstPage());
        assertNull(results.nextPage());

        // Now replay()
        ResultSet replayed = results.replay();
        assertEquals(1, replayed.getTotalHits());

        // Delete the resource and use replay() to check
        session.removeDocument(new PathRef("/testfolder1/testfile1"));
        session.save();
        replayed = results.replay();
        assertEquals(0, replayed.getTotalHits());
        assertEquals(0, replayed.getPageHits());
        assertEquals(0, replayed.getOffset());
        assertEquals(100, replayed.getRange());
        assertFalse(replayed.hasNextPage());
        assertTrue(replayed.isFirstPage());
    }

}
