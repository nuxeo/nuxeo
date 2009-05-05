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

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.picture.api.ImagingService;
import org.nuxeo.ecm.platform.picture.api.MetadataConstants;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * @author Laurent Doguin
 *
 */
public class TestMetaDataService extends NXRuntimeTestCase {

    ImagingService service;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.platform.picture.core");
        deployBundle("org.nuxeo.ecm.platform.picture.api");
        service = Framework.getService(ImagingService.class);
        assertNotNull(service);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        service = null;
    }

    private static File getFileFromPath(String path) {
        File file = FileUtils.getResourceFileFromContext(path);
        assertTrue(file.length() > 0);
        return file;
    }

    public void testMetaData() throws ClientException, IOException {
        File file = getFileFromPath("iptc_sample.jpg");
        Map<String, Object> map = service.getImageMetadata(file);
        assertNotNull(map);
        assertFalse(map.isEmpty());
        assertNotNull(map.get(MetadataConstants.META_BYLINE));
        assertNotNull(map.get(MetadataConstants.META_CAPTION));
        assertNotNull(map.get(MetadataConstants.META_CATEGORY));
        assertNotNull(map.get(MetadataConstants.META_CITY));
        assertNotNull(map.get(MetadataConstants.META_COUNTRY));
        assertNotNull(map.get(MetadataConstants.META_CREDIT));
        assertNotNull(map.get(MetadataConstants.META_DATE));
        assertNotNull(map.get(MetadataConstants.META_HEADLINE));
        assertNotNull(map.get(MetadataConstants.META_HEIGHT));
        assertNotNull(map.get(MetadataConstants.META_OBJECTNAME));
        assertNotNull(map.get(MetadataConstants.META_ORIGINALDATE));
        assertNotNull(map.get(MetadataConstants.META_SOURCE));
        assertNotNull(map.get(MetadataConstants.META_SUPPLEMENTALCATEGORIES));
        assertNotNull(map.get(MetadataConstants.META_WIDTH));

//      those metadata are not found by the parser
//        assertNotNull(map.get(MetadataConstants.META_COMMENT));
//        assertNotNull(map.get(MetadataConstants.META_COLORSPACE));
//        assertNotNull(map.get(MetadataConstants.META_COPYRIGHT));
//        assertNotNull(map.get(MetadataConstants.META_DESCRIPTION));
//        assertNotNull(map.get(MetadataConstants.META_EQUIPMENT));
//        assertNotNull(map.get(MetadataConstants.META_EXPOSURE));
//        assertNotNull(map.get(MetadataConstants.META_FOCALLENGTH));
//        assertNotNull(map.get(MetadataConstants.META_HRESOLUTION));
//        assertNotNull(map.get(MetadataConstants.META_ICCPROFILE));
//        assertNotNull(map.get(MetadataConstants.META_LANGUAGE));
//        assertNotNull(map.get(MetadataConstants.META_ISOSPEED));
//        assertNotNull(map.get(MetadataConstants.META_VRESOLUTION));
//        assertNotNull(map.get(MetadataConstants.META_WHITEBALANCE));
    }

}
