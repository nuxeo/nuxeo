/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.platform.thumbnail.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailAdapter;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailFactory;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Test thumbnail factories contributions
 *
 * @since 5.7
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.thumbnail")
@Deploy("org.nuxeo.ecm.platform.commandline.executor")
@Deploy("org.nuxeo.ecm.platform.convert")
@Deploy("org.nuxeo.ecm.platform.url")
@Deploy("org.nuxeo.ecm.platform.thumbnail:test-thumbnail-factories-contrib.xml")
public class TestThumbnailFactories {

    protected static Blob folderishThumbnail = Blobs.createBlob("folderish");

    protected static Blob defaultThumbnail = Blobs.createBlob("default");

    @Inject
    protected CoreSession session;

    @Test
    public void testThumbnailFactoryContribution() throws IOException {
        // Test folderish thumbnail factory
        DocumentModel root = session.getRootDocument();
        DocumentModel folder = session.createDocumentModel(root.getPathAsString(), "Folder", "Folder");
        folder.addFacet(FacetNames.FOLDERISH);
        session.createDocument(folder);
        session.save();
        ThumbnailAdapter folderThumbnail = folder.getAdapter(ThumbnailAdapter.class);
        assertEquals("folderish", folderThumbnail.getThumbnail(session).getString());

        // Test document thumbnail factory
        DocumentModel doc = session.createDocumentModel(root.getPathAsString(), "File", "File");
        session.createDocument(doc);
        session.save();
        ThumbnailAdapter docThumbnail = doc.getAdapter(ThumbnailAdapter.class);
        assertEquals("default", docThumbnail.getThumbnail(session).getString());
    }

    public static class DocumentTypeThumbnailFolderishFactory implements ThumbnailFactory {

        @Override
        public Blob getThumbnail(DocumentModel doc, CoreSession session) {
            if (!doc.isFolder()) {
                throw new NuxeoException("Document is not folderish");
            }
            return folderishThumbnail;
        }

        @Override
        public Blob computeThumbnail(DocumentModel doc, CoreSession session) {
            return null;
        }

    }

    public static class DocumentTypeThumbnailDocumentFactory implements ThumbnailFactory {
        @Override
        public Blob getThumbnail(DocumentModel doc, CoreSession session) {
            return defaultThumbnail;
        }

        @Override
        public Blob computeThumbnail(DocumentModel doc, CoreSession session) {
            return null;
        }
    }

}
