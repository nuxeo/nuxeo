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

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublishedDocument;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.content.template", //
        "org.nuxeo.ecm.platform.types.api", //
        "org.nuxeo.ecm.platform.types.core", //
        "org.nuxeo.ecm.platform.versioning.api", //
        "org.nuxeo.ecm.platform.versioning", //
        "org.nuxeo.ecm.platform.query.api", //
        "org.nuxeo.ecm.platform.publisher.core.contrib", //
})
@LocalDeploy({ "org.nuxeo.ecm.platform.publisher.core:OSGI-INF/publisher-framework.xml",
        "org.nuxeo.ecm.platform.publisher.core:OSGI-INF/publisher-contrib.xml",
        "org.nuxeo.ecm.platform.publisher.core:OSGI-INF/publisher-pageprovider-contrib.xml",
        "org.nuxeo.ecm.platform.publisher.test:OSGI-INF/publisher-remote-contrib-test.xml" })
public class TestRealRemoting {

    @Inject
    protected CoreSession session;

    DocumentModel doc2Publish;

    protected void createInitialDocs() throws Exception {

        DocumentModel wsRoot = session.getDocument(new PathRef("/default-domain/workspaces"));

        DocumentModel ws = session.createDocumentModel(wsRoot.getPathAsString(), "ws1", "Workspace");
        ws.setProperty("dublincore", "title", "test WS");
        ws = session.createDocument(ws);

        doc2Publish = session.createDocumentModel(ws.getPathAsString(), "file", "File");
        doc2Publish.setProperty("dublincore", "title", "MyDoc");

        Blob blob = Blobs.createBlob("SomeDummyContent");
        blob.setFilename("dummyBlob.txt");
        doc2Publish.setProperty("file", "content", blob);

        doc2Publish = session.createDocument(doc2Publish);

        session.save();
    }

    protected boolean skipRemoteTest = true;

    @Test
    public void testRemoting() throws Exception {
        if (skipRemoteTest) {
            return;
        }

        createInitialDocs();

        // check service config
        PublisherService service = Framework.getLocalService(PublisherService.class);
        List<String> treeNames = service.getAvailablePublicationTree();
        assertTrue(treeNames.contains("ClientRemoteTree2"));

        // check publication tree
        PublicationTree tree = service.getPublicationTree("ClientRemoteTree2", session, null);
        assertNotNull(tree);
        assertEquals("ClientRemoteTree2", tree.getConfigName());

        List<PublicationNode> sections = tree.getChildrenNodes();

        assertNotNull(sections);

        assertEquals("grzimek", sections.get(0).getTitle());
        assertEquals("section1", sections.get(1).getTitle());
        assertEquals("section2", sections.get(2).getTitle());

        // check treeconfigName propagation
        assertEquals(tree.getConfigName(), tree.getTreeConfigName());
        assertEquals(tree.getConfigName(), sections.get(1).getTreeConfigName());

        assertEquals(tree.getSessionId(), sections.get(1).getSessionId());

        // check publishing
        PublishedDocument pubDoc = tree.publish(doc2Publish, sections.get(1));
        assertNotNull(pubDoc);
        session.save();

        // check unpublishing
        List<PublishedDocument> publishedDocuments = tree.getExistingPublishedDocument(new DocumentLocationImpl(
                doc2Publish));
        assertEquals(1, publishedDocuments.size());
        tree.unpublish(pubDoc);
        publishedDocuments = tree.getExistingPublishedDocument(new DocumentLocationImpl(doc2Publish));
        assertEquals(0, publishedDocuments.size());
    }

}
