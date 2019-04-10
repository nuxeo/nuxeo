/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
