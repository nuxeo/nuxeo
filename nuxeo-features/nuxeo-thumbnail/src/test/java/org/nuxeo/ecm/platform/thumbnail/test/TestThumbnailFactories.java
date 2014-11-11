/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.platform.thumbnail.test;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailAdapter;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailFactory;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * Test thumbnail factories contributions
 * 
 * @since 5.7
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.thumbnail",
        "org.nuxeo.ecm.platform.commandline.executor",
        "org.nuxeo.ecm.platform.url.core", "org.nuxeo.ecm.platform.web.common" })
@LocalDeploy({ "org.nuxeo.ecm.platform.thumbnail:test-thumbnail-factories-contrib.xml" })
public class TestThumbnailFactories {

    protected static Blob folderishThumbnail = new StringBlob("folderish");

    protected static Blob defaultThumbnail = new StringBlob("default");

    @Inject
    CoreSession session;

    @Test
    public void testThumbnailFactoryContribution() throws ClientException,
            IOException {
        // Test folderish thumbnail factory
        DocumentModel root = session.getRootDocument();
        DocumentModel folder = new DocumentModelImpl(root.getPathAsString(),
                "Folder", "Folder");
        folder.addFacet(FacetNames.FOLDERISH);
        session.createDocument(folder);
        session.save();
        ThumbnailAdapter folderThumbnail = folder.getAdapter(ThumbnailAdapter.class);
        Assert.assertEquals(folderThumbnail.getThumbnail(session).getString(),
                "folderish");

        // Test document thumbnail factory
        DocumentModel doc = new DocumentModelImpl(root.getPathAsString(),
                "File", "File");
        session.createDocument(doc);
        session.save();
        ThumbnailAdapter docThumbnail = doc.getAdapter(ThumbnailAdapter.class);
        Assert.assertEquals(docThumbnail.getThumbnail(session).getString(),
                "default");
    }

    public static class DocumentTypeThumbnailFolderishFactory implements
            ThumbnailFactory {

        public Blob getThumbnail(DocumentModel doc, CoreSession session)
                throws ClientException {
            if (!doc.isFolder()) {
                throw new ClientException("Document is not folderish");
            }
            return folderishThumbnail;
        }

        @Override
        public Blob computeThumbnail(DocumentModel doc, CoreSession session) {
            return null;
        }

    }

    public static class DocumentTypeThumbnailDocumentFactory implements
            ThumbnailFactory {
        public Blob getThumbnail(DocumentModel doc, CoreSession session)
                throws ClientException {
            return defaultThumbnail;
        }

        @Override
        public Blob computeThumbnail(DocumentModel doc, CoreSession session) {
            return null;
        }
    }

}