/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.CoreIODocumentModelMarshaler;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.DefaultMarshaler;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.basic.BasicPublicationNode;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.basic.BasicPublishedDocument;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.DocumentModelMarshaler;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.RemotePublisherMarshaler;

/**
 * Test marshaling with real {@link DocumentModel}
 *
 * @author tiry
 *
 */
public class TestMashalingWithCore extends SQLRepositoryTestCase {

    DocumentModel doc2Export;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.io");
        deployBundle("org.nuxeo.ecm.platform.content.template");
        deployBundle("org.nuxeo.ecm.platform.types.api");
        deployBundle("org.nuxeo.ecm.platform.types.core");
        deployBundle("org.nuxeo.ecm.platform.versioning");
        deployBundle("org.nuxeo.ecm.platform.versioning.api");
        fireFrameworkStarted();
        openSession();
        createInitialDocs();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    protected void createInitialDocs() throws Exception {

        DocumentModel wsRoot = session.getDocument(new PathRef(
                "default-domain/workspaces"));

        DocumentModel ws = session.createDocumentModel(
                wsRoot.getPathAsString(), "ws1", "Workspace");
        ws.setProperty("dublincore", "title", "test WS");
        ws = session.createDocument(ws);

        doc2Export = session.createDocumentModel(ws.getPathAsString(), "file",
                "File");
        doc2Export.setProperty("dublincore", "title", "MyDoc");

        Blob blob = new StringBlob("SomeDummyContent");
        blob.setFilename("dummyBlob.txt");
        blob.setMimeType("text/plain");
        doc2Export.setProperty("file", "content", blob);
        doc2Export = session.createDocument(doc2Export);

        session.save();
    }

    @Test
    public void testMarshaling() throws Exception {
        DocumentModelMarshaler marshaler = new CoreIODocumentModelMarshaler();
        String docTitle = (String) doc2Export.getProperty("dublincore", "title");
        assertNotNull(docTitle);
        String data = marshaler.marshalDocument(doc2Export);
        assertNotNull(data);

        // System.out.println(data);

        session.removeDocument(doc2Export.getRef());
        session.save();

        DocumentModel doc = marshaler.unMarshalDocument(data, session);
        assertNotNull(doc);
        assertEquals(docTitle, doc.getTitle());

        Blob blob = (Blob) doc.getProperty("file", "content");
        assertNotNull(blob);

        assertEquals("SomeDummyContent", blob.getString());
    }

    @Test
    public void testComplexMarshaling() throws ClientException {
        RemotePublisherMarshaler marshaler = new DefaultMarshaler();
        marshaler.setAssociatedCoreSession(session);

        List<Object> params = new ArrayList<Object>();

        params.add("SessionId");
        params.add(new BasicPublicationNode("myType", "/some/path",
                "nodeTitle", "treeName"));
        params.add(new BasicPublishedDocument(new IdRef("id0"), "demorepo",
                "remoteServer", "version1", "path0", "parentPath0", false));

        List<PublicationNode> nodeList = new ArrayList<PublicationNode>();
        List<PublishedDocument> pubDocList = new ArrayList<PublishedDocument>();
        for (int i = 0; i < 5; i++) {
            nodeList.add(new BasicPublicationNode("myType", "/some/path/" + i,
                    "nodeTitle" + i, "treeName"));
            pubDocList.add(new BasicPublishedDocument(new IdRef("id0" + i),
                    "demorepo", "remoteServer", "version1", "path0" + i,
                    "parentPath0" + i, false));
        }

        params.add(nodeList);
        params.add(pubDocList);
        params.add(doc2Export);

        Map<String, String> opts = new HashMap<String, String>();
        opts.put("key1", "val1");
        opts.put("key2", "val2");
        opts.put("key3", "val3");
        params.add(opts);

        String data = marshaler.marshallParameters(params);

        assertNotNull(data);

        // System.out.println(data);

        List<Object> params2 = marshaler.unMarshallParameters(data);
        assertNotNull(params2);

        assertEquals(params.size(), params2.size());
        assertEquals(params.get(0), params2.get(0));
        assertEquals(((BasicPublicationNode) params.get(1)).getPath(),
                ((BasicPublicationNode) params2.get(1)).getPath());
        assertEquals(
                ((BasicPublishedDocument) params.get(2)).getSourceDocumentRef(),
                ((BasicPublishedDocument) params2.get(2)).getSourceDocumentRef());
        assertEquals(((BasicPublishedDocument) params.get(2)).isPending(),
                ((BasicPublishedDocument) params2.get(2)).isPending());
        for (int i = 0; i < 5; i++) {
            assertEquals(
                    ((BasicPublicationNode) ((List) params.get(3)).get(i)).getPath(),
                    ((BasicPublicationNode) ((List) params2.get(3)).get(i)).getPath());
            assertEquals(
                    ((BasicPublishedDocument) ((List) params.get(4)).get(i)).getSourceDocumentRef(),
                    ((BasicPublishedDocument) ((List) params2.get(4)).get(i)).getSourceDocumentRef());
        }
        assertEquals(((DocumentModel) params.get(5)).getTitle(),
                ((DocumentModel) params2.get(5)).getTitle());
        assertEquals(((Map<String, String>) params.get(6)).get("key2"),
                ((Map<String, String>) params2.get(6)).get("key2"));

        String data2 = marshaler.marshallResult(params);
        assertNotNull(data2);
        Object result = marshaler.unMarshallResult(data2);
        assertNotNull(result);

        List<Object> params3 = (List<Object>) result;
        assertEquals(params.size(), params3.size());
        assertEquals(params.get(0), params3.get(0));
        assertEquals(((BasicPublicationNode) params.get(1)).getPath(),
                ((BasicPublicationNode) params3.get(1)).getPath());
        assertEquals(
                ((BasicPublishedDocument) params.get(2)).getSourceDocumentRef(),
                ((BasicPublishedDocument) params3.get(2)).getSourceDocumentRef());
        for (int i = 0; i < 5; i++) {
            assertEquals(
                    ((BasicPublicationNode) ((List) params.get(3)).get(i)).getPath(),
                    ((BasicPublicationNode) ((List) params3.get(3)).get(i)).getPath());
            assertEquals(
                    ((BasicPublishedDocument) ((List) params.get(4)).get(i)).getSourceDocumentRef(),
                    ((BasicPublishedDocument) ((List) params3.get(4)).get(i)).getSourceDocumentRef());
        }
        assertEquals(((DocumentModel) params.get(5)).getTitle(),
                ((DocumentModel) params3.get(5)).getTitle());
        assertEquals(((Map<String, String>) params.get(6)).get("key2"),
                ((Map<String, String>) params3.get(6)).get("key2"));

    }

    @Test
    public void testMarshalingWithSource() throws Exception {
        DocumentModelMarshaler marshaler = new CoreIODocumentModelMarshaler();

        marshaler.setOriginatingServer("MySourceServer");

        String data = marshaler.marshalDocument(doc2Export);
        assertNotNull(data);

        // System.out.println(data);

        assertTrue(data.contains("<dc:source><![CDATA[test@MySourceServer:"));

        String srcRef = "test@MySourceServer:57583948-fc6a-41c1-9fa2-1be0aab12784";
        String[] refParts = srcRef.split("@");
        String sourceServer = refParts[1].split(":")[0];
        String repositoryName = refParts[0];
        DocumentRef ref = new IdRef(refParts[1].split(":")[1]);

        // System.out.println(sourceServer);
        // System.out.println(repositoryName);
        // System.out.println(ref);

    }
}
