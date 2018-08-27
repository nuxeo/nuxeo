/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.rendition.publisher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_FACET;
import static org.nuxeo.ecm.platform.rendition.Constants.RENDITION_SCHEMA;
import static org.nuxeo.ecm.platform.rendition.publisher.RenditionPublicationFactory.RENDITION_NAME_PARAMETER_KEY;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.publisher.api.PublicationNode;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;
import org.nuxeo.ecm.platform.publisher.api.PublisherService;
import org.nuxeo.ecm.platform.publisher.impl.core.SimpleCorePublishedDocument;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(init = RenditionPublicationRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.core.convert.api")
@Deploy("org.nuxeo.ecm.core.convert")
@Deploy("org.nuxeo.ecm.core.convert.plugins")
@Deploy("org.nuxeo.ecm.platform.convert")
@Deploy("org.nuxeo.ecm.platform.query.api")
@Deploy("org.nuxeo.ecm.platform.rendition.api")
@Deploy("org.nuxeo.ecm.platform.rendition.core")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.platform.versioning.api")
@Deploy("org.nuxeo.ecm.platform.versioning")
@Deploy("org.nuxeo.ecm.relations")
@Deploy("org.nuxeo.ecm.relations.jena")
@Deploy("org.nuxeo.ecm.platform.publisher.core.contrib")
@Deploy("org.nuxeo.ecm.platform.publisher.core")
@Deploy("org.nuxeo.ecm.platform.publisher.task")
@Deploy("org.nuxeo.ecm.platform.task.core")
@Deploy("org.nuxeo.ecm.platform.task.testing")
@Deploy("org.nuxeo.ecm.platform.rendition.publisher")
@Deploy("org.nuxeo.ecm.actions")
@Deploy("org.nuxeo.ecm.platform.rendition.publisher:relations-default-jena-contrib.xml")
public class TestRenditionPublication {

    @Inject
    protected CoreSession session;

    @Inject
    protected PublisherService publisherService;

    @Test
    public void shouldPublishASimpleProxyIfNoRenditionNameIsDefined() {
        String defaultTreeName = publisherService.getAvailablePublicationTree().get(0);
        PublicationTree tree = publisherService.getPublicationTree(defaultTreeName, session, null);

        List<PublicationNode> nodes = tree.getChildrenNodes();
        assertEquals(1, nodes.size());
        assertEquals("Section1", nodes.get(0).getTitle());

        PublicationNode targetNode = nodes.get(0);
        assertTrue(tree.canPublishTo(targetNode));

        DocumentModel file = session.createDocumentModel("/", "dummy", "File");
        file = session.createDocument(file);
        session.save();

        SimpleCorePublishedDocument publishedDocument = (SimpleCorePublishedDocument) tree.publish(file, targetNode);

        DocumentModel proxy = publishedDocument.getProxy();
        assertFalse(proxy.hasFacet(RENDITION_FACET));
        assertFalse(proxy.hasSchema(RENDITION_SCHEMA));
    }

    @Test
    public void shouldPublishAPDFRendition() {
        String defaultTreeName = publisherService.getAvailablePublicationTree().get(0);
        PublicationTree tree = publisherService.getPublicationTree(defaultTreeName, session, null);

        List<PublicationNode> nodes = tree.getChildrenNodes();
        assertEquals(1, nodes.size());
        assertEquals("Section1", nodes.get(0).getTitle());

        PublicationNode targetNode = nodes.get(0);
        assertTrue(tree.canPublishTo(targetNode));

        DocumentModel file = session.createDocumentModel("/", "dummy", "File");
        BlobHolder bh = file.getAdapter(BlobHolder.class);
        bh.setBlob(createTextBlob("dummy text", "dummy.txt"));
        file = session.createDocument(file);
        session.save();

        String liveUUID = file.getId();

        SimpleCorePublishedDocument publishedDocument = (SimpleCorePublishedDocument) tree.publish(file, targetNode,
                Collections.singletonMap(RENDITION_NAME_PARAMETER_KEY, "pdf"));

        DocumentModel proxy = publishedDocument.getProxy();
        String proxyId = proxy.getId();
        String proxySourceId = proxy.getSourceId();

        List<DocumentModel> versions = session.getVersions(file.getRef());
        assertEquals(1, versions.size());
        String versionUUID = versions.get(0).getId();

        assertTrue(proxy.hasFacet(RENDITION_FACET));
        assertTrue(proxy.hasSchema(RENDITION_SCHEMA));

        bh = proxy.getAdapter(BlobHolder.class);
        Blob renditionBlob = bh.getBlob();
        assertNotNull(renditionBlob);
        assertEquals("application/pdf", renditionBlob.getMimeType());

        RenditionService renditionService = Framework.getService(RenditionService.class);
        Rendition rendition = renditionService.getRendition(file, "pdf");
        assertNotNull(rendition);

        DocumentModel renditionDoc = rendition.getHostDocument();
        assertNotNull(renditionDoc);
        assertTrue(renditionDoc.isVersion());
        assertFalse(renditionDoc.isCheckedOut());
        String renditionUUID = renditionDoc.getId();

        assertEquals(proxySourceId, renditionUUID);

        // update the source document
        file.setPropertyValue("dc:description", "Updated");
        file = session.saveDocument(file);

        // now republish
        publishedDocument = (SimpleCorePublishedDocument) tree.publish(file, targetNode,
                Collections.singletonMap(RENDITION_NAME_PARAMETER_KEY, "pdf"));

        versions = session.getVersions(file.getRef());
        assertEquals(2, versions.size());

        // verify that the proxy has been updqted
        proxy = publishedDocument.getProxy();
        assertEquals(proxyId, proxy.getId());

        rendition = renditionService.getRendition(file, "pdf");
        renditionDoc = rendition.getHostDocument();

        assertEquals(renditionDoc.getId(), proxy.getSourceId());
        assertNotEquals(renditionDoc.getId(), renditionUUID);

    }

    protected Blob createTextBlob(String content, String filename) {
        Blob blob = Blobs.createBlob(content);
        blob.setFilename(filename);
        return blob;
    }

}
