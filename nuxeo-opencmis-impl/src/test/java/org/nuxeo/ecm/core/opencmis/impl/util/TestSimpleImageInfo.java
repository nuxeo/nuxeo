/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.core.opencmis.impl.util;

import java.io.InputStream;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TestSimpleImageInfo {

    public void check(String name, long length, String mimeType) throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream(name);
        SimpleImageInfo info = new SimpleImageInfo(is);
        assertEquals(800, info.getWidth());
        assertEquals(600, info.getHeight());
        assertEquals(length, info.getLength());
        assertEquals(mimeType, info.getMimeType());
    }

    @Test
    public void testSimpleImageInfo() throws Exception {
        check("big_nuxeo_logo.gif", 11426, "image/gif");
        check("big_nuxeo_logo.jpg", 36830, "image/jpeg");
        check("big_nuxeo_logo.png", 7939, "image/png");
    }

}
