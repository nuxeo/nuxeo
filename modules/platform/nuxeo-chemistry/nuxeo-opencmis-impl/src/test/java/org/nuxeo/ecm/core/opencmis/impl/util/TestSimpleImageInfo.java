/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
