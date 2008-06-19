/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 *
 * $Id$
 */

package org.nuxeo.common.utils;

import junit.framework.TestCase;

/**
 * @author Florent Guillaume
 */
public class TestRFC2231 extends TestCase {

    public void testEncodeWithPercent() throws Exception {
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

    public void testEncodeContentDisposition() throws Exception {
        assertEquals("inline; filename*=UTF-8''caf%C3%A9;",
                RFC2231.encodeContentDisposition("caf\u00e9", true, "Firefox"));
        assertEquals("attachment; filename=caf%C3%A9;",
                RFC2231.encodeContentDisposition("caf\u00e9", false, "MSIE"));
        assertEquals("attachment; filename=caf\u00e9;",
                RFC2231.encodeContentDisposition("caf\u00e9", false, null));
        assertEquals(
                "attachment; filename*=UTF-8''R%C3%A9sultat%20d%27Activit%C3%A9%20%28%3Bprovisoire/draft%29.;",
                RFC2231.encodeContentDisposition(
                        "R\u00e9sultat d'Activit\u00e9 (;provisoire/draft).",
                        false, "Firefox"));
    }

}
