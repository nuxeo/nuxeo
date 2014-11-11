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
import java.io.Serializable;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.InputStreamBlob;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.storage.sql.ra.PoolingRepositoryFactory;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.thumbnail.ThumbnailConstants;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.inject.Inject;

/**
 * Test thumbnail storage for doctype File
 *
 * @since 5.7
 */
@RunWith(FeaturesRunner.class)
@Features({ TransactionalFeature.class, CoreFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD, repositoryFactoryClass = PoolingRepositoryFactory.class)
@Deploy({ "org.nuxeo.ecm.platform.thumbnail",
        "org.nuxeo.ecm.platform.commandline.executor",
        "org.nuxeo.ecm.platform.convert", "org.nuxeo.ecm.platform.url.core",
        "org.nuxeo.ecm.platform.web.common" })
@LocalDeploy("org.nuxeo.ecm.platform.thumbnail:test-thumbnail-listener-contrib.xml")
public class TestThumbnailStorage {

    @Inject
    CoreSession session;

    @Inject
    EventService eventService;

    @Before
    public void resetUpdateCount() {
        UpdateThumbnailCounter.count = 0;
    }

    @Test
    public void testCreation() throws ClientException, IOException {
        DocumentModel root = session.getRootDocument();
        DocumentModel file = new DocumentModelImpl(root.getPathAsString(),
                "File", "File");
        // Attach a blob
        Blob blob = new InputStreamBlob(TestThumbnailStorage.class.getResource(
                "/test-data/big_nuxeo_logo.jpg").openStream(), "image/jpeg",
                null, "logo.jpg", null);
        file.setPropertyValue("file:content", (Serializable) blob);
        file = session.createDocument(file);
        TransactionHelper.commitOrRollbackTransaction();

        eventService.waitForAsyncCompletion(); // wait for thumbnail update

        TransactionHelper.startTransaction();

        file = session.getDocument(file.getRef());
        Assert.assertTrue(file.hasFacet(ThumbnailConstants.THUMBNAIL_FACET));
        Assert.assertNotNull(file.getPropertyValue(ThumbnailConstants.THUMBNAIL_PROPERTY_NAME));
        Assert.assertEquals(1, UpdateThumbnailCounter.count);
    }

    @Test
    public void testUpdate() throws ClientException, IOException {
        DocumentModel root = session.getRootDocument();
        DocumentModel file = new DocumentModelImpl(root.getPathAsString(),
                "File", "File");
        file = session.createDocument(file);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion(); // wait for thumbnail update
        TransactionHelper.startTransaction();

        Assert.assertFalse(file.hasFacet(ThumbnailConstants.THUMBNAIL_FACET));

        // Attach a blob
        Blob blob = new InputStreamBlob(TestThumbnailStorage.class.getResource(
                "/test-data/big_nuxeo_logo.jpg").openStream(), "image/jpeg",
                null, "logo.jpg", null);
        file.setPropertyValue("file:content", (Serializable) blob);
        file = session.saveDocument(file);

        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion(); // wait for thumbnail update
        TransactionHelper.startTransaction();

        file = session.getDocument(file.getRef());

        Assert.assertTrue(file.hasFacet(ThumbnailConstants.THUMBNAIL_FACET));
        Assert.assertNotNull(file.getPropertyValue(ThumbnailConstants.THUMBNAIL_PROPERTY_NAME));
        Assert.assertEquals(1, UpdateThumbnailCounter.count);
    }
}
