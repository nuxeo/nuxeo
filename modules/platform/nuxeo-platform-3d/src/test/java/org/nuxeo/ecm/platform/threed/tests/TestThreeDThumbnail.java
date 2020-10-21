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

import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.platform.threed.ThreeDDocumentConstants.RENDER_VIEWS_PROPERTY;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailAdapter;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.threed.BatchConverterHelper;
import org.nuxeo.ecm.platform.threed.ThreeD;
import org.nuxeo.ecm.platform.threed.ThreeDDocument;
import org.nuxeo.ecm.platform.threed.ThreeDRenderView;
import org.nuxeo.ecm.platform.threed.service.ThreeDService;
import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

/**
 * Test 3D thumbnail
 *
 * @since 8.4
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.commandline.executor")
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.platform.picture.core")
@Deploy("org.nuxeo.ecm.platform.picture.api")
@Deploy("org.nuxeo.ecm.platform.picture.convert")
@Deploy("org.nuxeo.ecm.platform.rendition.core")
@Deploy("org.nuxeo.ecm.platform.threed")
@Deploy("org.nuxeo.ecm.platform.threed.test:OSGI-INF/threed-disable-listeners-contrib.xml")
@ConditionalIgnoreRule.Ignore(condition = ConditionalIgnoreRule.IgnoreWindows.class)
public class TestThreeDThumbnail {

    private static final Log log = LogFactory.getLog(TestThreeDThumbnail.class);

    @Inject
    protected CoreSession session;

    @Inject
    protected RuntimeHarness runtimeHarness;

    @Inject
    protected ThreeDService threeDService;

    protected void updateThreeDDocument(DocumentModel doc, ThreeD threeD) {
        BlobHolder results = threeDService.batchConvert(threeD);
        List<ThreeDRenderView> threeDRenderViews = BatchConverterHelper.getRenders(results);
        var renderViewList = threeDRenderViews.stream().map(ThreeDRenderView::toMap).collect(Collectors.toList());
        doc.setPropertyValue(RENDER_VIEWS_PROPERTY, (Serializable) renderViewList);
    }

    @Test
    public void testPictureThumbnail() throws Exception {
        DocumentModel threed = session.createDocumentModel("/", "threed", "ThreeD");
        threed = session.createDocument(threed);
        session.save();
        Blob blob = Blobs.createBlob(FileUtils.getResourceFileFromContext("test-data/suzanne.obj"), "image/gif", null,
                "suzanne.obj");
        runtimeHarness.deployContrib("org.nuxeo.ecm.platform.threed.test",
                "OSGI-INF/threed-service-contrib-override.xml");
        long begin = System.currentTimeMillis();
        updateThreeDDocument(threed, new ThreeD(blob, null, null));
        long timeDelta = System.currentTimeMillis() - begin;
        session.saveDocument(threed);
        session.save();

        // Thumbnail service should return the default picture thumbnail
        ThumbnailAdapter pictureThumbnail = threed.getAdapter(ThumbnailAdapter.class);
        if (threed.getProperty(RENDER_VIEWS_PROPERTY).size() == 0) {
            log.warn(String.format("[NXP-21450] memory max: %dMB", Runtime.getRuntime().maxMemory() / 1024 / 1024));
            log.warn(String.format("[NXP-21450] memory total: %dMB", Runtime.getRuntime().totalMemory() / 1024 / 1024));
            log.warn(String.format("[NXP-21450] memory free: %dMB", Runtime.getRuntime().freeMemory() / 1024 / 1024));
            log.warn(String.format("[NXP-21450] duration: %dms", timeDelta));
        }
        Collection<ThreeDRenderView> renderViews = threed.getAdapter(ThreeDDocument.class).getRenderViews();
        Blob pictureUsualThumbnail = renderViews.iterator().next().getContent();
        assertEquals(pictureUsualThumbnail.getFilename(), pictureThumbnail.getThumbnail(session).getFilename());
        runtimeHarness.undeployContrib("org.nuxeo.ecm.platform.threed.test",
                "OSGI-INF/threed-service-contrib-override.xml");
    }
}
