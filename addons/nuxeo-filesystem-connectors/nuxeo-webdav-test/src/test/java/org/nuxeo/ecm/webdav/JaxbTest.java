/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.webdav;

import net.java.dev.webdav.jaxrs.xml.elements.PropFind;
import net.java.dev.webdav.jaxrs.xml.elements.PropertyUpdate;

import org.junit.Test;
import org.nuxeo.ecm.webdav.jaxrs.Util;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import java.io.InputStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * Tests that some sample files are properly parsed by JAXB.
 */
public class JaxbTest {

    @Test
    public void testPropFind() throws Exception {
        testFile("propfind1.xml", PropFind.class);
        testFile("propfind2.xml", PropFind.class);
        testFile("propfind3.xml", PropFind.class);
        testFile("propertyupdate1.xml", PropertyUpdate.class);
    }

    private void testFile(String name, Class<?> class_) throws Exception {
        JAXBContext jc = Util.getJaxbContext();
        Unmarshaller u = jc.createUnmarshaller();

        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("xmlsamples/" + name);
        assertNotNull(is);
        Object o = u.unmarshal(is);
        assertSame(o.getClass(), class_);
    }

}
