/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.runtime.datasource;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.naming.RefAddr;
import javax.naming.Reference;

import org.junit.Test;
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestDataSourceDescriptor extends NXRuntimeTestCase {

    protected static DataSourceDescriptor load(String resource)
            throws Exception {
        URL url = Thread.currentThread().getContextClassLoader().getResource(
                resource);
        XMap xmap = new XMap();
        xmap.register(DataSourceDescriptor.class);
        return (DataSourceDescriptor) xmap.load(url);
    }

    @Test
    public void testDataSourceDescriptor() throws Exception {
        DataSourceDescriptor descr = load("test-datasource-descriptor.xml");
        assertEquals("foo", descr.name);
        Reference ref = descr.getReference();
        Map<String, String> expected = new HashMap<String, String>();
        expected.put("maxIdle", "123");
        expected.put("bar", "gee");
        expected.put("baz", "moo");
        Map<String, String> actual = new HashMap<String, String>();
        for (RefAddr addr : Collections.list(ref.getAll())) {
            actual.put(addr.getType(), (String) addr.getContent());
        }
        assertEquals(expected, actual);
    }

}
