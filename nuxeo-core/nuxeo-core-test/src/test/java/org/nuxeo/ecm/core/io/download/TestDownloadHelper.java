/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
