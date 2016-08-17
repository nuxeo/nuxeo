/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Tiago Cardoso <tcardoso@nuxeo.com>
 */
package org.nuxeo.ecm.platform.threed.tests;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailAdapter;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.picture.api.PictureView;
import org.nuxeo.ecm.platform.picture.api.adapters.MultiviewPicture;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;
import org.nuxeo.ecm.platform.threed.BatchConverterHelper;
import org.nuxeo.ecm.platform.threed.ThreeD;
import org.nuxeo.ecm.platform.threed.ThreeDRenderView;
import org.nuxeo.ecm.platform.threed.service.ThreeDService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.platform.picture.api.adapters.AbstractPictureAdapter.SMALL_SIZE;
import static org.nuxeo.ecm.platform.threed.ThreeDConstants.THUMBNAIL_PICTURE_TITLE;

/**
 * Test 3D thumbnail
 *
 * @since 8.4
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.commandline.executor", "org.nuxeo.ecm.automation.core",
        "org.nuxeo.ecm.platform.picture.core", "org.nuxeo.ecm.platform.picture.api",
        "org.nuxeo.ecm.platform.picture.convert", "org.nuxeo.ecm.platform.rendition.core" })
@LocalDeploy({ "org.nuxeo.ecm.platform.threed.api", "org.nuxeo.ecm.platform.threed.core",
        "org.nuxeo.ecm.platform.threed.convert" })
public class TestThreeDThumbnail {

    @Inject
    protected CoreSession session;

    @Inject
    protected ThreeDService threeDService;

    @Inject
    protected EventServiceAdmin eventServiceAdmin;

    private static File getFileFromPath(String path) {
        return FileUtils.getResourceFileFromContext(path);
    }

    @Before
    public void setUp() {
        eventServiceAdmin.setListenerEnabledFlag("threeDBatchGenerationListener", false);
    }

    protected void updateThreeDDocument(DocumentModel doc, ThreeD threeD) throws IOException {
        Collection<Blob> results = threeDService.batchConvert(threeD);

        ThreeDRenderView threeDRenderView = BatchConverterHelper.getRenders(results).get(0);
        PictureResourceAdapter picture = doc.getAdapter(PictureResourceAdapter.class);
        ArrayList<Map<String, Object>> thumbnailTemplates = new ArrayList<>();
        Map<String, Object> thumbnailView = new LinkedHashMap<>();
        thumbnailView.put("title", THUMBNAIL_PICTURE_TITLE);
        thumbnailView.put("maxsize", (long) SMALL_SIZE);
        thumbnailTemplates.add(thumbnailView);

        picture.fillPictureViews(threeDRenderView.getContent(), threeDRenderView.getContent().getFilename(),
                threeDRenderView.getTitle(), new ArrayList<>(thumbnailTemplates));

    }

    @Test
    public void testPictureThumbnail() throws Exception {
        DocumentModel threed = session.createDocumentModel("/", "threed", "ThreeD");
        threed = session.createDocument(threed);
        session.save();
        Blob blob = Blobs.createBlob(getFileFromPath("test-data/suzane.obj"), "image/gif", null, "suzane.obj");
        updateThreeDDocument(threed, new ThreeD(blob, null));
        session.saveDocument(threed);
        session.save();

        // Get picture thumbnail view
        PictureResourceAdapter picture = threed.getAdapter(PictureResourceAdapter.class);
        Blob pictureUsualThumbnail = picture.getPictureFromTitle(THUMBNAIL_PICTURE_TITLE);

        // Thumbnail service should return the default picture thumbnail
        ThumbnailAdapter pictureThumbnail = threed.getAdapter(ThumbnailAdapter.class);
        assertEquals(pictureUsualThumbnail.getFilename(), pictureThumbnail.getThumbnail(session).getFilename());
    }
}