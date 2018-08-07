/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.picture.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Calendar;

import javax.inject.Inject;

import org.apache.commons.io.FilenameUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.platform.picture.api.PictureView;
import org.nuxeo.ecm.platform.picture.api.PictureViewImpl;
import org.nuxeo.ecm.platform.picture.api.adapters.MultiviewPicture;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.commandline.executor")
@Deploy("org.nuxeo.ecm.actions")
@Deploy("org.nuxeo.ecm.platform.picture.api")
@Deploy("org.nuxeo.ecm.platform.picture.core")
@Deploy("org.nuxeo.ecm.platform.picture.convert")
@Deploy("org.nuxeo.ecm.platform.tag")
@Deploy("org.nuxeo.ecm.platform.collections.core:OSGI-INF/collection-core-types-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.picture.core:OSGI-INF/imaging-listeners-override.xml")
public class TestImagingAdapter {

    private static final String JPEG_IMAGE = "iptc_sample.jpg";

    @Inject
    protected CoreSession session;

    @Inject
    protected ImagingService imagingService;

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
            Blob blob = Blobs.createBlob(ImagingResourcesHelper.getFileFromPath(path));
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
        return Framework.getService(ImagingService.class).getConfigurationValue("conversionFormat", "jpg");
    }

    @Test
    public void testMultiviewPictureAdapter() throws IOException {
        DocumentModel picture = session.createDocumentModel("/", "picture", "Picture");
        picture = session.createDocument(picture);

        MultiviewPicture mvp = picture.getAdapter(MultiviewPicture.class);
        PictureView[] views = mvp.getViews();
        assertEquals(5, views.length);

        String path = ImagingResourcesHelper.TEST_DATA_FOLDER + "cat.gif";
        Blob blob = Blobs.createBlob(ImagingResourcesHelper.getFileFromPath(path));
        ImageInfo info = imagingService.getImageInfo(blob);
        PictureView view = new PictureViewImpl();
        view.setBlob(blob);
        view.setDescription("a view");
        view.setFilename("cat.gif");
        view.setHeight(info.getHeight());
        view.setImageInfo(info);
        view.setTitle("a view");
        view.setWidth(info.getWidth());
        mvp.addView(view);
        session.saveDocument(picture);

        // refetch the Picture
        picture = session.getDocument(picture.getRef());
        mvp = picture.getAdapter(MultiviewPicture.class);
        views = mvp.getViews();
        assertEquals(6, views.length);

        view = mvp.getView("a view");
        info = view.getImageInfo();
        assertNotEquals(0, info.getWidth());
        assertNotEquals(0, info.getHeight());
        assertNotEquals(0, info.getDepth());
        assertNotNull(info.getColorSpace());
        assertNotNull(info.getFormat());

        view = mvp.getView("Thumbnail");
        info = view.getImageInfo();
        assertNotEquals(0, info.getWidth());
        assertNotEquals(0, info.getHeight());
        assertNotEquals(0, info.getDepth());
        assertNotNull(info.getColorSpace());
        assertNotNull(info.getFormat());
    }
}
