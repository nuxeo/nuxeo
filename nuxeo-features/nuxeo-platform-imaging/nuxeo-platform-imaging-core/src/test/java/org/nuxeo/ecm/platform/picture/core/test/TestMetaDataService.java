/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.picture.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.binary.metadata.test.BinaryMetadataFeature;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.platform.picture.api.PrefixMetadataConstants;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @author Laurent Doguin
 */
@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class, BinaryMetadataFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy({ "org.nuxeo.ecm.platform.picture.api", "org.nuxeo.ecm.core.convert",
        "org.nuxeo.ecm.platform.commandline.executor", "org.nuxeo.ecm.platform.picture.core",
        "org.nuxeo.ecm.platform.picture.convert" })
public class TestMetaDataService {

    @Inject
    protected ImagingService service;

    @Inject
    protected CoreSession session;

    private List<Map<String, Serializable>> createViews() {
        List<Map<String, Serializable>> views = new ArrayList<Map<String, Serializable>>();
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.put("title", "Original");
        map.put("content",
                new FileBlob(
                        FileUtils.getResourceFileFromContext(ImagingResourcesHelper.TEST_DATA_FOLDER + "test.jpg"),
                        "image/jpeg", null, "test.jpg", null));
        map.put("filename", "test.jpg");
        views.add(map);
        return views;
    }


    private static File getFileFromPath(String path) {
        File file = FileUtils.getResourceFileFromContext(path);
        assertTrue(file.length() > 0);
        return file;
    }

    @Test
    public void testMetaData() throws Exception {
        Blob blob = new FileBlob(getFileFromPath("images/iptc_sample.jpg"));
        blob.setMimeType("image/jpeg");
        Map<String, Object> map = service.getImageMetadata(blob);
        assertNotNull(map);
        assertFalse(map.isEmpty());
        assertNotNull(map.get(PrefixMetadataConstants.META_BY_LINE));
        assertNotNull(map.get(PrefixMetadataConstants.META_CAPTION));
        assertNotNull(map.get(PrefixMetadataConstants.META_CATEGORY));
        assertNotNull(map.get(PrefixMetadataConstants.META_CITY));
        assertNotNull(map.get(PrefixMetadataConstants.META_COUNTRY_OR_PRIMARY_LOCATION));
        assertNotNull(map.get(PrefixMetadataConstants.META_CREDIT));
        assertNotNull(map.get(PrefixMetadataConstants.META_DATE_CREATED));
        assertNotNull(map.get(PrefixMetadataConstants.META_HEADLINE));
        assertNotNull(map.get(PrefixMetadataConstants.META_HEIGHT));
        assertNotNull(map.get(PrefixMetadataConstants.META_OBJECT_NAME));
        assertNotNull(map.get(PrefixMetadataConstants.META_SOURCE));
        assertNotNull(map.get(PrefixMetadataConstants.META_SUPPLEMENTAL_CATEGORIES));
        assertNotNull(map.get(PrefixMetadataConstants.META_WIDTH));
    }

    @Test
    public void testIPTCMetadataMapping() {
        DocumentModel picture = new DocumentModelImpl(session.getRootDocument().getPathAsString(), "pic", "Picture");
        picture.setPropertyValue("picture:views", (Serializable) createViews());
        picture = session.createDocument(picture);
        session.save();

        BlobHolder bh = picture.getAdapter(BlobHolder.class);
        assertNotNull(bh);
        Blob blob = bh.getBlob();
        assertNull(blob);
        assertEquals(1, bh.getBlobs().size());

        blob = new FileBlob(getFileFromPath("images/test.jpg"), "image/jpeg", null, "test.jpg", null);
        bh.setBlob(blob);
        session.saveDocument(picture);
        session.save();

        assertEquals("Horizontal (normal)", picture.getPropertyValue("imd:orientation"));
        assertEquals(72L, picture.getPropertyValue("imd:xresolution"));
        assertEquals("125", picture.getPropertyValue("imd:iso_speed_ratings"));
        assertEquals("sRGB", picture.getPropertyValue("imd:color_space"));
        assertEquals("Auto", picture.getPropertyValue("imd:white_balance"));
        assertEquals(5.6, picture.getPropertyValue("imd:fnumber"));
    }

}
