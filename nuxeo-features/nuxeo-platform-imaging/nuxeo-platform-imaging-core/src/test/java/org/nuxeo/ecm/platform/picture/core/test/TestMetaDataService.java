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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.binary.metadata.test.BinaryMetadataFeature;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
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
@Features({ CoreFeature.class, BinaryMetadataFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.commandline.executor", "org.nuxeo.ecm.platform.picture.core",
        "org.nuxeo.ecm.platform.picture.api" })
public class TestMetaDataService {

    @Inject
    protected ImagingService service;

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

}
