/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.core.util.ComplexTypeJSONDecoder;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @since 10.2
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.core:test-blobprovider.xml")
public class TestComplexTypeJSONDecoder {

    @Test
    public void testDecodeManagedBlob() throws Exception {

        String emptyKeyJson = "{\"providerId\":\"testBlobProvider\"}";
        ObjectMapper om = new ObjectMapper();

        Blob blob = ComplexTypeJSONDecoder.getBlobFromJSON((ObjectNode) om.readTree(emptyKeyJson));
        assertNull(blob);

        String unknownProviderJson = "{\"providerId\":\"fakeBlobProvider\", \"key\":\"testKey\"}";
        blob = ComplexTypeJSONDecoder.getBlobFromJSON((ObjectNode) om.readTree(unknownProviderJson));
        assertNull(blob);

        String json = "{\"providerId\":\"testBlobProvider\", \"key\":\"testKey\"}";
        blob = ComplexTypeJSONDecoder.getBlobFromJSON((ObjectNode) om.readTree(json));
        assertTrue(blob instanceof ManagedBlob);
        ManagedBlob managedBlob = (ManagedBlob) blob;
        assertEquals("testBlobProvider", managedBlob.getProviderId());
        assertEquals("testBlobProvider:testKey", managedBlob.getKey());

    }

}
