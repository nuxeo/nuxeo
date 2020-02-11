/*
 * (C) Copyright 2006-2020 Nuxeo (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.core.convert.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.convert.cache.CacheKeyGenerator;
import org.nuxeo.ecm.core.convert.cache.ConversionCacheGCManager;
import org.nuxeo.ecm.core.convert.cache.ConversionCacheHolder;
import org.nuxeo.ecm.core.convert.cache.SimpleCachableBlobHolder;
import org.nuxeo.ecm.core.convert.extension.Converter;
import org.nuxeo.ecm.core.convert.service.ConversionServiceImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(ConvertFeature.class)
@Deploy("org.nuxeo.ecm.core.convert:OSGI-INF/convert-service-config-enabled.xml")
@Deploy("org.nuxeo.ecm.core.convert:OSGI-INF/converters-test-contrib3.xml")
public class TestCache {

    @Test
    public void testCache() throws Exception {
        ConversionService cs = Framework.getService(ConversionService.class);

        Converter cv = ConversionServiceImpl.getConverter("identity");
        assertNotNull(cv);

        int cacheSize1 = ConversionCacheHolder.getNbCacheEntries();
        long cacheHits1 = ConversionCacheHolder.getCacheHits();

        File file = FileUtils.getResourceFileFromContext("test-data/hello.doc");
        assertNotNull(file);
        assertTrue(file.length() > 0);

        Blob blob = Blobs.createBlob(file, "application/msword", null, "hello.doc");

        BlobHolder bh = new SimpleBlobHolder(blob);

        BlobHolder result = cs.convert("identity", bh, null);

        assertNotNull(result);

        int cacheSize2 = ConversionCacheHolder.getNbCacheEntries();

        // check new cache entry was created
        assertEquals(1, cacheSize2 - cacheSize1);

        cs.convert("identity", bh, null);

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

    @Test
    public void shouldNotReturnNullFileCacheEntries() throws IOException {
        File file = FileUtils.getResourceFileFromContext("test-data/hello.doc");

        // Put a blobholder in the cache
        Blob blob = Blobs.createBlob(file);
        BlobHolder bh = new SimpleCachableBlobHolder(blob);
        String cacheKey = CacheKeyGenerator.computeKey("identity", bh, null);
        ConversionCacheHolder.addToCache(cacheKey, bh);

        // check new cache entry was created
        assertEquals(1, ConversionCacheHolder.getNbCacheEntries());

        // retrieve the cache entry
        BlobHolder blobHolder = ConversionCacheHolder.getFromCache(cacheKey);

        // delete the temp file
        Files.delete(Paths.get(blobHolder.getBlob().getFile().getPath()));
        // check the cache is outdated
        assertEquals(1, ConversionCacheHolder.getNbCacheEntries());
        assertTrue(ConversionCacheHolder.getCacheKeys().contains(cacheKey));

        // requesting a null file cache entry returns null after clearing the outdated key
        assertNull(ConversionCacheHolder.getFromCache(cacheKey));
        assertEquals(0, ConversionCacheHolder.getNbCacheEntries());
    }

}
