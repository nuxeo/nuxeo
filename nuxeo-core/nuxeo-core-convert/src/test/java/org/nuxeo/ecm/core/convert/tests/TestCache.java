/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.core.convert.tests;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.cache.ConversionCacheGCManager;
import org.nuxeo.ecm.core.convert.cache.ConversionCacheHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.service.ConversionServiceImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestCache extends NXRuntimeTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("org.nuxeo.ecm.core.api");
        deployBundle("org.nuxeo.ecm.core.convert.api");
        deployBundle("org.nuxeo.ecm.core.convert");
        deployContrib("org.nuxeo.ecm.core.convert.tests", "OSGI-INF/convert-service-config-enabled.xml");
    }

    @Test
    public void testCache() throws Exception {
        deployContrib("org.nuxeo.ecm.core.convert.tests", "OSGI-INF/converters-test-contrib3.xml");
        ConversionService cs = Framework.getLocalService(ConversionService.class);

        Converter cv = ConversionServiceImpl.getConverter("identity");
        assertNotNull(cv);

        int cacheSize1 = ConversionCacheHolder.getNbCacheEntries();
        long cacheHits1 = ConversionCacheHolder.getCacheHits();

        File file = FileUtils.getResourceFileFromContext("test-data/hello.doc");
        assertNotNull(file);
        assertTrue(file.length() > 0);

        Blob blob = new FileBlob(file);
        blob.setFilename("hello.doc");
        blob.setMimeType("application/msword");

        BlobHolder bh = new SimpleBlobHolder(blob);

        BlobHolder result = cs.convert("identity", bh, null);

        assertNotNull(result);

        int cacheSize2 = ConversionCacheHolder.getNbCacheEntries();

        // check new cache entry was created
        assertEquals(1, cacheSize2 - cacheSize1);

        BlobHolder result2 = cs.convert("identity", bh, null);

        // check NO new cache entry was created
        cacheSize2 = ConversionCacheHolder.getNbCacheEntries();
        assertEquals(1, cacheSize2 - cacheSize1);

        long cacheHits2 = ConversionCacheHolder.getCacheHits();

        // check cache hits
        assertEquals(1, cacheHits2 - cacheHits1);

        // force GC
        ConversionCacheGCManager.doGC(file.length() / 1024);

        int cacheSize3 = ConversionCacheHolder.getNbCacheEntries();
        assertEquals(0, cacheSize1 - cacheSize3);
    }

}
