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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * Test thumbnail storage for doctype File
 * 
 * @since 5.7
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.thumbnail",
        "org.nuxeo.ecm.platform.commandline.executor",
        "org.nuxeo.ecm.platform.url.core", "org.nuxeo.ecm.platform.web.common" })
public class TestThumbnailStorage {

    @Inject
    CoreSession session;

    @Inject
    EventService eventService;

    @Test
    @Ignore
    public void testThumbnailCRUD() throws ClientException, IOException {
        DocumentModel root = session.getRootDocument();
        DocumentModel file = new DocumentModelImpl(root.getPathAsString(),
                "File", "File");
        // Attach a blob
        Blob blob = new FileBlob(
                getFileFromPath("test-data/big_nuxeo_logo.jpg"), "image/jpeg",
                null, "logo.jpg", null);
        file.setPropertyValue("file:content", (Serializable) blob);
        file = session.createDocument(file);
        session.save();
        Assert.assertTrue(file.hasFacet(ThumbnailConstants.THUMBNAIL_FACET));

        eventService.waitForAsyncCompletion();
        session.save();
        // Check if thumbnail has been created and stored
        DocumentModel sameFile = session.getDocument(file.getRef());
        Assert.assertNotNull((Blob) sameFile.getPropertyValue(ThumbnailConstants.THUMBNAIL_PROPERTY_NAME));
    }

    private static File getFileFromPath(String path) {
        return FileUtils.getResourceFileFromContext(path);
    }
}