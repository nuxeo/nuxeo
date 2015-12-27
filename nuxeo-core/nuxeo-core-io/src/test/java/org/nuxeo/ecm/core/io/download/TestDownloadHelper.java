/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.io.download;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.nuxeo.ecm.core.io.download.DownloadService.ByteRange;

public class TestDownloadHelper {

    @Test
    public void testParseByteRange() throws Exception {
        ByteRange byteRange = DownloadHelper.parseRange("bytes=42-169", 12345);
        assertEquals(42, byteRange.getStart());
        assertEquals(169, byteRange.getEnd());
        assertEquals(128, byteRange.getLength());
    }

    @Test
    public void testParseByteRangeWithoutEnd() throws Exception {
        ByteRange byteRange = DownloadHelper.parseRange("bytes=0-", 12345);
        assertEquals(0, byteRange.getStart());
        assertEquals(12344, byteRange.getEnd());
        assertEquals(12345, byteRange.getLength());
    }

    @Test
    public void testParseByteRangeWithoutStart() throws Exception {
        ByteRange byteRange = DownloadHelper.parseRange("bytes=-128", 12345);
        assertEquals(12217, byteRange.getStart());
        assertEquals(12344, byteRange.getEnd());
        assertEquals(128, byteRange.getLength());
    }

    @Test
    public void testParseUnsupportedByteRange() throws Exception {
        assertNull(DownloadHelper.parseRange("blablabla", 12345));
    }

    @Test
    public void testParseUnsupportedByteRange2() throws Exception {
        assertNull(DownloadHelper.parseRange("bytes=", 12345));
    }

    @Test
    public void testParseUnsupportedByteRange3() throws Exception {
        assertNull(DownloadHelper.parseRange("bytes=-", 12345));
    }

    @Test
    public void testParseUnsupportedByteRange4() throws Exception {
        assertNull(DownloadHelper.parseRange("bytes=123-45", 12345)); // Start > end
    }

    @Test
    public void testParseUnsupportedByteRange5() throws Exception {
        assertNull(DownloadHelper.parseRange("bytes=0-123,-45", 12345)); // Do no support multiple ranges
    }

    @Test
    public void testParseUnsupportedByteRange6() throws Exception {
        assertNull(DownloadHelper.parseRange("bytes=foo-bar", 12345));
    }

}
