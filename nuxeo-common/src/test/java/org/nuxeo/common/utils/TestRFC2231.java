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

    @Test
    public void testEncodeWithPercent() {
        StringBuilder buf = new StringBuilder();
        RFC2231.percentEscape(buf, null);
        assertEquals("", buf.toString());
        RFC2231.percentEscape(buf, "");
        assertEquals("", buf.toString());
        RFC2231.percentEscape(buf, "foo");
        assertEquals("foo", buf.toString());
        buf.setLength(0);
        RFC2231.percentEscape(buf, "foo bar");
        assertEquals("foo%20bar", buf.toString());
        buf.setLength(0);
        RFC2231.percentEscape(buf, "R\u00e9sultat d'Activit\u00e9 (;provisoire/draft).");
        assertEquals("R%C3%A9sultat%20d%27Activit%C3%A9%20%28%3Bprovisoire/draft%29.", buf.toString());
    }

    @Test
    public void testEncodeContentDisposition() throws Exception {
        assertEquals("inline; filename*=UTF-8''caf%C3%A9",
                RFC2231.encodeContentDisposition("caf\u00e9", true, TestUserAgent.FF_30));
        assertEquals("attachment; filename=caf%C3%A9",
                RFC2231.encodeContentDisposition("caf\u00e9", false, TestUserAgent.MSIE7_UA));
        assertEquals("attachment; filename*=UTF-8''caf%C3%A9", RFC2231.encodeContentDisposition("caf\u00e9", false, null));
        assertEquals("attachment; filename*=UTF-8''R%C3%A9sultat%20d%27Activit%C3%A9%20%28%3Bprovisoire/draft%29.",
                RFC2231.encodeContentDisposition("R\u00e9sultat d'Activit\u00e9 (;provisoire/draft).", false,
                        TestUserAgent.FF_30));
        assertEquals("attachment; filename*=UTF-8''%E5%B9%B3%E4%BB%AE%E5%90%8D%20-%20good.txt",
                RFC2231.encodeContentDisposition("\u5e73\u4eee\u540d - good.txt", false, TestUserAgent.MSIE11));
        assertEquals(
                "attachment; filename*=UTF-8''%E3%83%8C%E3%82%AF%E3%82%BB%E3%82%AA%E3%83%BB%E3%82%B7%E3%82%99%E3%83%A3%E3%83%8F%E3%82%9A%E3%83%B3.txt",
                RFC2231.encodeContentDisposition(
                        "\u30cc\u30af\u30bb\u30aa\u30fb\u30b7\u3099\u30e3\u30cf\u309a\u30f3.txt", false,
                        TestUserAgent.SAFARI11));
    }

}
