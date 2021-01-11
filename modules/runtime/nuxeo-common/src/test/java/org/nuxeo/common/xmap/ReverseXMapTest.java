/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.common.xmap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class ReverseXMapTest {

    @Test
    public void testReverse() throws IOException {
        XMap xmap = new XMap();
        xmap.register(Author.class);
        URL url = Thread.currentThread().getContextClassLoader().getResource("test-xmap.xml");
        Author author = (Author) xmap.load(url);
        try {
            xmap.toXML("");
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            assertTrue(msg, msg.contains("java.lang.String is NOT registred in xmap"));
        }

        String content = xmap.toXML(author);

        URL refurl = Thread.currentThread().getContextClassLoader().getResource("test-xmap-saved.xml");
        String refcontent = IOUtils.toString(refurl, StandardCharsets.UTF_8);

        assertEquals(refcontent.trim(), content.trim());
    }

}
