/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Florent Guillaume
 */
public class TestRFC2231 {

    @Test
    public void testEncodeWithPercent() {
        StringBuilder buf = new StringBuilder();
        RFC2231.percentEscape(buf, "foo");
        assertEquals("foo", buf.toString());
        buf.setLength(0);
        RFC2231.percentEscape(buf, "foo bar");
        assertEquals("foo%20bar", buf.toString());
        buf.setLength(0);
        RFC2231.percentEscape(
                buf, "R\u00e9sultat d'Activit\u00e9 (;provisoire/draft).");
        assertEquals(
                "R%C3%A9sultat%20d%27Activit%C3%A9%20%28%3Bprovisoire/draft%29.",
                buf.toString());
    }

    @Test
    public void testEncodeContentDisposition() throws Exception {
        assertEquals("inline; filename*=UTF-8''caf%C3%A9",
                RFC2231.encodeContentDisposition("caf\u00e9", true, "Firefox"));
        assertEquals("attachment; filename=caf%C3%A9",
                RFC2231.encodeContentDisposition("caf\u00e9", false, "MSIE"));
        assertEquals("attachment; filename=caf\u00e9",
                RFC2231.encodeContentDisposition("caf\u00e9", false, null));
        assertEquals(
                "attachment; filename*=UTF-8''R%C3%A9sultat%20d%27Activit%C3%A9%20%28%3Bprovisoire/draft%29.",
                RFC2231.encodeContentDisposition(
                        "R\u00e9sultat d'Activit\u00e9 (;provisoire/draft).",
                        false, "Firefox"));
    }

}
