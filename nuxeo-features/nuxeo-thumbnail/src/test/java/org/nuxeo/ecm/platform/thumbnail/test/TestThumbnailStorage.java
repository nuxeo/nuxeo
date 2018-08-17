/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.IOException;
import java.io.Serializable;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * Test thumbnail storage for doctype File
 *
 * @since 5.7
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.thumbnail", //
        "org.nuxeo.ecm.platform.commandline.executor", //
        "org.nuxeo.ecm.platform.convert", //
        "org.nuxeo.ecm.platform.url.core", //
        "org.nuxeo.ecm.platform.web.common" //
})
@LocalDeploy("org.nuxeo.ecm.platform.thumbnail:test-thumbnail-listener-contrib.xml")
public class TestThumbnailStorage {

    @Inject
    protected CoreSession session;

    @Inject
    protected EventService eventService;

    @Inject
    protected TransactionalFeature txFeature;

    @Before
    public void resetUpdateCount() {
        UpdateThumbnailCounter.count = 0;
    }

    @Test
    public void testCreation() throws IOException {
        DocumentModel file = session.createDocumentModel("/", "File", "File");
        // Attach a blob
        Blob blob = Blobs.createBlob(FileUtils.getResourceFileFromContext("test-data/big_nuxeo_logo.jpg"),
                "image/jpeg");
        file.setPropertyValue("file:content", (Serializable) blob);
        file = session.createDocument(file);

        txFeature.nextTransaction(); // wait for thumbnail update

        file = session.getDocument(file.getRef());
        Assert.assertTrue(file.hasFacet(ThumbnailConstants.THUMBNAIL_FACET));
        Assert.assertNotNull(file.getPropertyValue(ThumbnailConstants.THUMBNAIL_PROPERTY_NAME));
        Assert.assertEquals(1, UpdateThumbnailCounter.count);
    }

    @Test
    public void testUpdate() throws IOException {
        DocumentModel file = session.createDocumentModel("/", "File", "File");
        file = session.createDocument(file);

        txFeature.nextTransaction(); // wait for thumbnail update

        Assert.assertFalse(file.hasFacet(ThumbnailConstants.THUMBNAIL_FACET));

        // Attach a blob
        Blob blob = Blobs.createBlob(FileUtils.getResourceFileFromContext("test-data/big_nuxeo_logo.jpg"),
                "image/jpeg");
        file.setPropertyValue("file:content", (Serializable) blob);
        file = session.saveDocument(file);

        txFeature.nextTransaction(); // wait for thumbnail update

        file = session.getDocument(file.getRef());
        Assert.assertTrue(file.hasFacet(ThumbnailConstants.THUMBNAIL_FACET));
        Assert.assertNotNull(file.getPropertyValue(ThumbnailConstants.THUMBNAIL_PROPERTY_NAME));
        Assert.assertEquals(1, UpdateThumbnailCounter.count);

        // update a property other than the content, shouldn't trigger thumbnail computation
        file.setPropertyValue("dc:title", "Update title");
        file = session.saveDocument(file);

        txFeature.nextTransaction(); // wait for thumbnail update

        file = session.getDocument(file.getRef());
        Assert.assertEquals(1, UpdateThumbnailCounter.count);
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.thumbnail:test-thumbnail-update-filename-listener-contrib.xml")
    public void testAvoidThumbnailUpdateLoop() throws IOException {
        // Create
        DocumentModel doc = session.createDocumentModel("/", "testDoc", "File");
        Blob blob = Blobs.createBlob(FileUtils.getResourceFileFromContext("test-data/big_nuxeo_logo.jpg"),
                "image/jpeg");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.createDocument(doc);

        txFeature.nextTransaction(); // wait for thumbnail update

        Assert.assertEquals(1, UpdateThumbnailCounter.count);

        // Update
        blob.setFilename("logo.jpg");
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc = session.saveDocument(doc);

        txFeature.nextTransaction(); // wait for thumbnail update

        Assert.assertEquals(2, UpdateThumbnailCounter.count);
    }
}
