/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.output.NullOutputStream.NULL_OUTPUT_STREAM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.blob.KeyStrategy.WriteObserver;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestKeyStrategies {

    protected static final String ABC = "abc";

    protected static final String ABC_MD5 = "900150983cd24fb0d6963f7d28e17f72";

    @Test
    public void testKeyStrategyDigest() throws IOException {
        KeyStrategy ks = new KeyStrategyDigest("MD5");

        assertTrue(ks.useDeDuplication());
        assertEquals("deadbeef", ks.getDigestFromKey("deadbeef"));

        // write observer / key computer
        BlobContext blobContext = new BlobContext(null, "foo", "bar");
        BlobWriteContext c = ks.getBlobWriteContext(blobContext);
        WriteObserver writeObserver = c.writeObserver;
        @SuppressWarnings("resource")
        OutputStream out = writeObserver.wrap(NULL_OUTPUT_STREAM);
        out.write(ABC.getBytes(UTF_8), 0, 3);
        out.flush();
        writeObserver.done();
        assertEquals(ABC_MD5, c.getKey());

        // equals
        KeyStrategy ks2 = new KeyStrategyDigest("MD5");
        KeyStrategy ks3 = new KeyStrategyDigest("SHA-256");
        assertEquals(ks, ks);
        assertEquals(ks, ks2);
        assertNotEquals(ks, ks3);
        assertNotEquals(ks, "foobar");
    }

    @Test
    public void testKeyStrategyDocId() {
        KeyStrategy ks = KeyStrategyDocId.instance();

        assertFalse(ks.useDeDuplication());
        assertNull(ks.getDigestFromKey("deadbeef"));

        // write observer / key computer
        BlobContext blobContext = new BlobContext(null, "docid1", "content");
        BlobWriteContext c = ks.getBlobWriteContext(blobContext);
        assertNull(c.writeObserver);
        assertEquals("docid1", c.getKey());

        // equals
        KeyStrategy ks2 = new KeyStrategyDocId();
        assertEquals(ks, ks);
        assertEquals(ks, ks2);
        assertNotEquals(ks, "foobar");
    }
}
