/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(BlobManagerFeature.class)
@Deploy("org.nuxeo.ecm.core.api.tests:OSGI-INF/dummy-blob-provider.xml")
public class TestBlobManager {

    private static final String DUMMY = "dummy";

    @Inject
    protected BlobManager blobManager;

    @Test
    public void testDummyRegistration() throws Exception {
        BlobProvider dummyBlobProvider = blobManager.getBlobProvider(DUMMY);
        assertNotNull(dummyBlobProvider);
    }

    @Test
    public void testGetBlobProviders() throws Exception {
        Map<String, BlobProvider> providers = blobManager.getBlobProviders();
        assertEquals(3, providers.size()); // default, otherdefault and dummy
    }

    @Test
    public void testDefaultAndNamespace() throws Exception {
        BlobProvider def = blobManager.getBlobProvider("default");
        BlobProvider other1 = blobManager.getBlobProviderWithNamespace("providerNotRegisteredInXML", "default");
        BlobProvider other2 = blobManager.getBlobProviderWithNamespace("otherProviderNotRegisteredInXML", "otherdefault");
        assertNotNull(def);
        assertEquals(def.getClass(), other1.getClass());
        assertEquals(def.getClass(), other2.getClass());
        // check that the blob providers come from two different defaults
        assertEquals("main", other1.getProperties().get("kind"));
        assertEquals("other", other2.getProperties().get("kind"));
        // put a blob in the default one
        String key = def.writeBlob(new StringBlob("foo"));
        assertEquals("foo", readBlob(def, key));
        assertNull(readBlob(other1, key));
        assertNull(readBlob(other2, key));
        // put a blob in the first namespaced one
        String key2 = other1.writeBlob(new StringBlob("bar"));
        // make sure there's no key collision
        assertEquals("foo", readBlob(def, key));
        assertEquals("bar", readBlob(other1, key2));
        assertNull(readBlob(other2, key));
        assertNull(readBlob(other2, key2));
    }

    protected static String readBlob(BlobProvider blobProvider, String key) {
        try {
            BlobInfo blobInfo = new BlobInfo();
            blobInfo.key = key;
            Blob blob = blobProvider.readBlob(blobInfo);
            return blob.getString();
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().equals("Unknown blob: " + key));
            return null;
        }
    }

}
