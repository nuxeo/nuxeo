/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.CoreIODocumentModelMarshaler;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.DefaultMarshaler;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.basic.BasicPublicationNode;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.basic.BasicPublishedDocument;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.DocumentModelMarshaler;
import org.nuxeo.ecm.platform.publisher.remoting.marshaling.interfaces.RemotePublisherMarshaler;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Test marshaling with real {@link DocumentModel}
 *
 * @author tiry
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.core.io", //
        "org.nuxeo.ecm.platform.content.template", //
        "org.nuxeo.ecm.platform.types.api", //
        "org.nuxeo.ecm.platform.types.core", //
        "org.nuxeo.ecm.platform.versioning.api", //
        "org.nuxeo.ecm.platform.versioning", //
})
public class TestMashalingWithCore {

    @Inject
    protected CoreSession session;

    DocumentModel doc2Export;

    @Before
    public void setUp() throws Exception {
        createInitialDocs();
    }

    protected void createInitialDocs() throws Exception {

        DocumentModel wsRoot = session.getDocument(new PathRef("/default-domain/workspaces"));

        DocumentModel ws = session.createDocumentModel(wsRoot.getPathAsString(), "ws1", "Workspace");
        ws.setProperty("dublincore", "title", "test WS");
        ws = session.createDocument(ws);

        doc2Export = session.createDocumentModel(ws.getPathAsString(), "file", "File");
        doc2Export.setProperty("dublincore", "title", "MyDoc");

        Blob blob = Blobs.createBlob("SomeDummyContent");
        blob.setFilename("dummyBlob.txt");
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
    public void testComplexMarshaling() {
        RemotePublisherMarshaler marshaler = new DefaultMarshaler();
        marshaler.setAssociatedCoreSession(session);

        List<Object> params = new ArrayList<Object>();

        params.add("SessionId");
        params.add(new BasicPublicationNode("myType", "/some/path", "nodeTitle", "treeName"));
        params.add(new BasicPublishedDocument(new IdRef("id0"), "demorepo", "remoteServer", "version1", "path0",
                "parentPath0", false));

        List<PublicationNode> nodeList = new ArrayList<PublicationNode>();
        List<PublishedDocument> pubDocList = new ArrayList<PublishedDocument>();
        for (int i = 0; i < 5; i++) {
            nodeList.add(new BasicPublicationNode("myType", "/some/path/" + i, "nodeTitle" + i, "treeName"));
            pubDocList.add(new BasicPublishedDocument(new IdRef("id0" + i), "demorepo", "remoteServer", "version1",
                    "path0" + i, "parentPath0" + i, false));
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
        assertEquals(((BasicPublishedDocument) params.get(2)).getSourceDocumentRef(),
                ((BasicPublishedDocument) params2.get(2)).getSourceDocumentRef());
        assertEquals(((BasicPublishedDocument) params.get(2)).isPending(),
                ((BasicPublishedDocument) params2.get(2)).isPending());
        for (int i = 0; i < 5; i++) {
            assertEquals(((BasicPublicationNode) ((List) params.get(3)).get(i)).getPath(),
                    ((BasicPublicationNode) ((List) params2.get(3)).get(i)).getPath());
            assertEquals(((BasicPublishedDocument) ((List) params.get(4)).get(i)).getSourceDocumentRef(),
                    ((BasicPublishedDocument) ((List) params2.get(4)).get(i)).getSourceDocumentRef());
        }
        assertEquals(((DocumentModel) params.get(5)).getTitle(), ((DocumentModel) params2.get(5)).getTitle());
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
        assertEquals(((BasicPublishedDocument) params.get(2)).getSourceDocumentRef(),
                ((BasicPublishedDocument) params3.get(2)).getSourceDocumentRef());
        for (int i = 0; i < 5; i++) {
            assertEquals(((BasicPublicationNode) ((List) params.get(3)).get(i)).getPath(),
                    ((BasicPublicationNode) ((List) params3.get(3)).get(i)).getPath());
            assertEquals(((BasicPublishedDocument) ((List) params.get(4)).get(i)).getSourceDocumentRef(),
                    ((BasicPublishedDocument) ((List) params3.get(4)).get(i)).getSourceDocumentRef());
        }
        assertEquals(((DocumentModel) params.get(5)).getTitle(), ((DocumentModel) params3.get(5)).getTitle());
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
