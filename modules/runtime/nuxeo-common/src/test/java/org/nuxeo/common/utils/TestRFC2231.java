/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.common.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Florent Guillaume
 */
public class TestRFC2231 {

    @SuppressWarnings("removal")
    @Test
    public void testEncodeWithPercent() {
        StringBuilder sb = new StringBuilder();
        RFC2231.percentEscape(sb, "foo");
        assertEquals("foo", sb.toString());
        sb.setLength(0);
        RFC2231.percentEscape(sb, "foo bar");
        assertEquals("foo%20bar", sb.toString());
        sb.setLength(0);
        RFC2231.percentEscape(sb, "R\u00e9sultat d'Activit\u00e9 (;provisoire/draft).");
        assertEquals("R%C3%A9sultat%20d%27Activit%C3%A9%20%28%3Bprovisoire/draft%29.", sb.toString());
    }

    @Test
    public void testEncodeContentDisposition() throws Exception {
        // Pure token filenames - no quoting or encoding needed
        assertEquals("inline; filename=cafe", RFC2231.encodeContentDisposition("cafe", true));
        assertEquals("inline; filename=mydoc2.txt", RFC2231.encodeContentDisposition("mydoc2.txt", true));
        assertEquals("inline; filename=file+with+plus.tar.gz",
                RFC2231.encodeContentDisposition("file+with+plus.tar.gz", true));

        // Non-ASCII characters - require quoting and RFC 2231 encoding
        assertEquals("inline; filename=\"caf\u00e9\"; filename*=UTF-8''caf%C3%A9",
                RFC2231.encodeContentDisposition("caf\u00e9", true));
        assertEquals("attachment; filename=\"caf\u00e9\"; filename*=UTF-8''caf%C3%A9",
                RFC2231.encodeContentDisposition("caf\u00e9", false));
        assertEquals(
                "attachment; filename=\"\u5e73\u4eee\u540d - good.txt\"; filename*=UTF-8''%E5%B9%B3%E4%BB%AE%E5%90%8D%20-%20good.txt",
                RFC2231.encodeContentDisposition("\u5e73\u4eee\u540d - good.txt", false));
        assertEquals(
                "attachment; filename=\"\u30cc\u30af\u30bb\u30aa\u30fb\u30b7\u3099\u30e3\u30cf\u309a\u30f3.txt\"; filename*=UTF-8''%E3%83%8C%E3%82%AF%E3%82%BB%E3%82%AA%E3%83%BB%E3%82%B7%E3%82%99%E3%83%A3%E3%83%8F%E3%82%9A%E3%83%B3.txt",
                RFC2231.encodeContentDisposition(
                        "\u30cc\u30af\u30bb\u30aa\u30fb\u30b7\u3099\u30e3\u30cf\u309a\u30f3.txt", false));

        // Special characters requiring quoting and encoding
        assertEquals(
                "attachment; filename=\"R\u00e9sultat d'Activit\u00e9 (;provisoire/draft).\"; filename*=UTF-8''R%C3%A9sultat%20d%27Activit%C3%A9%20%28%3Bprovisoire%2Fdraft%29.",
                RFC2231.encodeContentDisposition("R\u00e9sultat d'Activit\u00e9 (;provisoire/draft).", false));
        assertEquals("inline; filename=\"test{test}.txt\"; filename*=UTF-8''test%7Btest%7D.txt",
                RFC2231.encodeContentDisposition("test{test}.txt", true));

        // Quote escaping - double-quotes must be escaped with backslash
        assertEquals("attachment; filename=\"Say \\\"Hello\\\".txt\"; filename*=UTF-8''Say%20%22Hello%22.txt",
                RFC2231.encodeContentDisposition("Say \"Hello\".txt", false));

        // Backslash escaping - backslashes must be escaped with backslash
        assertEquals("attachment; filename=\"path\\\\file.txt\"; filename*=UTF-8''path%5Cfile.txt",
                RFC2231.encodeContentDisposition("path\\file.txt", false));

        // Control character sanitization - CTL chars (0x00-0x1F, 0x7F) are stripped to prevent header injection
        assertEquals("attachment; filename=\"filenamewithCRLF.txt\"; filename*=UTF-8''filename%0D%0Awith%0D%0ACRLF.txt",
                RFC2231.encodeContentDisposition("filename\r\nwith\r\nCRLF.txt", false));
        assertEquals("attachment; filename=\"tabfile.txt\"; filename*=UTF-8''tab%09file.txt",
                RFC2231.encodeContentDisposition("tab\tfile.txt", false));
        assertEquals("attachment; filename=\"nullfile.txt\"; filename*=UTF-8''null%00file.txt",
                RFC2231.encodeContentDisposition("null\0file.txt", false));

        // Null and blank filename handling - defaults to "file" to prevent NPE
        assertEquals("inline; filename=file", RFC2231.encodeContentDisposition(null, true));
        assertEquals("attachment; filename=file", RFC2231.encodeContentDisposition("", false));
        assertEquals("attachment; filename=file", RFC2231.encodeContentDisposition("  ", false));
    }

}
