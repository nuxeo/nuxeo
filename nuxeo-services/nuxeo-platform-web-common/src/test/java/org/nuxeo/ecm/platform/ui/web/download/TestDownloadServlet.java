/*
 * (C) Copyright 2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.ui.web.download;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

public class TestDownloadServlet {

    @Test
    public void testParse() {
        assertParsed("blobholder:0", null, "");
        assertParsed("blobholder:0", null, "/");
        assertParsed("foo", null, "/foo");
        assertParsed("foo", null, "/foo/");
        assertParsed("foo", "bar", "/foo/bar");
        assertParsed("foo/bar", "baz", "/foo/bar/baz");
        assertParsed("foo/bar/baz", "moo", "/foo/bar/baz/moo");
    }

    protected static void assertParsed(String path, String filename, String string) {
        Pair<String, String> pair = DownloadServlet.parsePath("somerepo/someid" + string);
        assertEquals(path + " " + filename, pair.getLeft() + " " + pair.getRight());
    }

}
