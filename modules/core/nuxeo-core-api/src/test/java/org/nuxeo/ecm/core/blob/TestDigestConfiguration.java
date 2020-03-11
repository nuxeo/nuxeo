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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;

public class TestDigestConfiguration {

    @Test
    public void test() {
        DigestConfiguration dc;
        dc = new DigestConfiguration(null, Collections.singletonMap("digest", "MD5"));
        assertEquals("MD5", dc.digestAlgorithm);
        assertEquals("[0-9a-f]{32}", dc.digestPattern.toString());
        assertFalse(dc.isValidDigest("dead"));
        assertTrue(dc.isValidDigest("d41d8cd98f00b204e9800998ecf8427e"));
        dc = new DigestConfiguration(null, Collections.singletonMap("digest", "SHA-256"));
        assertEquals("SHA-256", dc.digestAlgorithm);
        assertEquals("[0-9a-f]{64}", dc.digestPattern.toString());
        assertFalse(dc.isValidDigest("dead"));
        assertTrue(dc.isValidDigest("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"));
        dc = new DigestConfiguration("SHA-512");
        assertEquals("SHA-512", dc.digestAlgorithm);
        assertEquals("[0-9a-f]{128}", dc.digestPattern.toString());
        assertFalse(dc.isValidDigest("dead"));
        assertTrue(dc.isValidDigest(
                "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e"));
    }

}
