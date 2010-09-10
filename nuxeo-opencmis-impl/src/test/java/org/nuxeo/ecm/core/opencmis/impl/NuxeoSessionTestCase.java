/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.core.opencmis.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Policy;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.opencmis.impl.client.NuxeoSession;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;

/**
 * Tests that hit the high-level Session abstraction.
 */
public abstract class NuxeoSessionTestCase extends SQLRepositoryTestCase {

    public static final String NUXEO_ROOT_TYPE = "Root"; // from Nuxeo

    public static final String NUXEO_ROOT_NAME = ""; // NuxeoPropertyDataName;

    public static final String USERNAME = "test";

    public static final String PASSWORD = "test";

    protected Session session;

    protected String rootFolderId;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // deployed for fulltext indexing
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployBundle("org.nuxeo.ecm.core.convert.plugins");

        openSession(); // nuxeo

        setUpCmisSession();

        setUpData();

        RepositoryInfo rid = session.getBinding().getRepositoryService().getRepositoryInfo(
                getRepositoryId(), null);
        assertNotNull(rid);
        rootFolderId = rid.getRootFolderId();
        assertNotNull(rootFolderId);
    }

    @Override
    public void tearDown() throws Exception {
        tearDownData();
        tearDownCmisSession();
        super.tearDown();
    }

    /** Sets up the client, fills "session". */
    public abstract void setUpCmisSession() throws Exception;

    /** Tears down the client. */
    public abstract void tearDownCmisSession() throws Exception;

    protected void setUpData() throws Exception {
        Helper.makeNuxeoRepository(super.session);
    }

    protected void tearDownData() {
    }

    protected CoreSession getCoreSession() {
        return super.session;
    }

    protected String getRepositoryId() {
        return super.session.getRepositoryName();
    }

    @Test
    public void testRoot() {
        Folder root = session.getRootFolder();
        assertNotNull(root);
        assertNotNull(root.getName());
        assertNotNull(root.getId());
        assertNull(root.getFolderParent());
        assertNotNull(root.getType());
        assertEquals(NUXEO_ROOT_TYPE, root.getType().getId());
        assertEquals(rootFolderId, root.getPropertyValue("cmis:objectId"));
        assertEquals(NUXEO_ROOT_TYPE,
                root.getPropertyValue("cmis:objectTypeId"));
        assertEquals(NUXEO_ROOT_NAME, root.getName());
        List<Property<?>> props = root.getProperties();
        assertNotNull(props);
        assertTrue(props.size() > 0);
    }

    @Test
    public void testCreateObject() {
        Folder root = session.getRootFolder();
        ContentStream contentStream = null;
        VersioningState versioningState = null;
        List<Policy> policies = null;
        List<Ace> addAces = null;
        List<Ace> removeAces = null;
        OperationContext context = NuxeoSession.DEFAULT_CONTEXT;
        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put("cmis:objectTypeId", "Note");
        properties.put("cmis:name", "mynote");
        properties.put("note", "bla bla");
        Document doc = root.createDocument(properties, contentStream,
                versioningState, policies, addAces, removeAces, context);
        assertNotNull(doc.getId());
        assertEquals("mynote", doc.getName());
        assertEquals("mynote", doc.getPropertyValue("dc:title"));
        assertEquals("bla bla", doc.getPropertyValue("note"));
    }

    public void testBasic() throws Exception {
        Folder root = session.getRootFolder();
        ItemIterable<CmisObject> entries = root.getChildren();
        assertEquals(2, entries.getTotalNumItems());

        Folder folder = (Folder) session.getObjectByPath("/testfolder1");
        assertNotNull(folder);
        Document file = null;
        for (CmisObject child : folder.getChildren()) {
            String name = child.getName();
            if (name.equals("testfile1_Title")) {
                file = (Document) child;
            }
        }
        assertNotNull(file);

        // get stream
        ContentStream cs = file.getContentStream();
        assertNotNull(cs);
        assertEquals("text/plain", cs.getMimeType());
        if (cs.getFileName() != null) {
            // TODO fix AtomPub case where the filename is null
            assertEquals("testfile.txt", cs.getFileName());
        }
        if (cs.getLength() != -1) {
            // TODO fix AtomPub case where the length is unknown (streaming)
            assertEquals(Helper.FILE1_CONTENT.length(), cs.getLength());
        }
        assertEquals(Helper.FILE1_CONTENT, FileUtils.read(cs.getStream()));

        // set stream
        // cs = new SimpleContentStream("foo".getBytes(), "text/html",
        // "foo.html");
        // file.setContentStream(cs);
        // file.save();
    }

}
