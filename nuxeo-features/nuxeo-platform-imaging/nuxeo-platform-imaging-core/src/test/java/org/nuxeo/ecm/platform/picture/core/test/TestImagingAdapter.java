/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.picture.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;

import org.apache.commons.io.FilenameUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.platform.picture.api.PictureView;
import org.nuxeo.ecm.platform.picture.api.adapters.MultiviewPicture;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.commandline.executor", "org.nuxeo.ecm.platform.mimetype.api",
        "org.nuxeo.ecm.platform.mimetype.core", "org.nuxeo.ecm.platform.picture.api",
        "org.nuxeo.ecm.platform.picture.core", "org.nuxeo.ecm.platform.picture.convert" })
public class TestImagingAdapter {

    private static final String JPEG_IMAGE = "iptc_sample.jpg";

    @Inject
    protected CoreSession session;

    @Test
    public void testAdapter() throws Exception {
        DocumentModel root = session.getRootDocument();
        DocumentModel folder = session.createDocumentModel(root.getPathAsString(), "document", "Folder");
        folder.setProperty("dublincore", "description", "toto");
        folder = session.createDocument(folder);

        DocumentModel child = session.createDocumentModel(folder.getPathAsString(), "fils" + 1, "Picture");
        child = session.createDocument(child);
        child.setProperty("dublincore", "description", "fils" + 1 + " description");
        child.setProperty("dublincore", "valid", Calendar.getInstance());

        PictureResourceAdapter adapter = child.getAdapter(PictureResourceAdapter.class);
        assertNotNull(adapter);

        for (String filename : ImagingResourcesHelper.TEST_IMAGE_FILENAMES) {
            String path = ImagingResourcesHelper.TEST_DATA_FOLDER + filename;
            Blob blob = new FileBlob(ImagingResourcesHelper.getFileFromPath(path));
            assertNotNull(blob);

            blob.setFilename(filename);

            boolean ret = adapter.fillPictureViews(blob, filename, filename);
            assertTrue(ret);

            child = session.saveDocument(child);
            session.save();

            adapter = child.getAdapter(PictureResourceAdapter.class);
            DocumentModel documentModel = session.getChildren(folder.getRef()).get(0);

            MultiviewPicture multiview = documentModel.getAdapter(MultiviewPicture.class);
            assertEquals(child.getRef(), documentModel.getRef());
            assertEquals(child, documentModel);

            MultiviewPicture adaptedChild = child.getAdapter(MultiviewPicture.class);
            assertNotNull(adaptedChild);

            /*
             * Test thumbnail
             */

            PictureView pictureView = adaptedChild.getView("Thumbnail");
            assertNotNull(pictureView);
            String computedFilename = FilenameUtils.getBaseName(filename) + "." + getConversionFormat();
            assertEquals("Thumbnail_" + computedFilename, pictureView.getFilename());
            assertEquals("Thumbnail_" + computedFilename, pictureView.getBlob().getFilename());

            pictureView = multiview.getView("Thumbnail");
            assertEquals("Thumbnail_" + computedFilename, pictureView.getFilename());
            assertEquals("Thumbnail_" + computedFilename, pictureView.getBlob().getFilename());

            /*
             * Test Small
             */

            pictureView = adaptedChild.getView("Small");
            assertNotNull(pictureView);
            assertEquals("Small_" + computedFilename, pictureView.getFilename());
            assertEquals("Small_" + computedFilename, pictureView.getBlob().getFilename());

            pictureView = multiview.getView("Small");
            assertEquals("Small_" + computedFilename, pictureView.getFilename());
            assertEquals("Small_" + computedFilename, pictureView.getBlob().getFilename());
            assertNotNull(pictureView);
        }
    }

    protected String getConversionFormat() {
        return Framework.getLocalService(ImagingService.class).getConfigurationValue("conversionFormat", "jpg");
    }
}
