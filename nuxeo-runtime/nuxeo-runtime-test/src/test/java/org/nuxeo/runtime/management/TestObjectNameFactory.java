/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.runtime.management;

import static org.junit.Assert.assertEquals;

import javax.management.ObjectName;

import org.junit.Test;

/**
 * @author matic
 */
public class TestObjectNameFactory {

    @Test
    public void testSimpleForm() {
        ObjectName name = ObjectNameFactory.getObjectName("simple");
        assertEquals("org.nuxeo:name=simple,type=service", name.getCanonicalName());
    }

    @Test
    public void testAvaForm() {
        ObjectName name = ObjectNameFactory.getObjectName("name=value");
        assertEquals("org.nuxeo:name=value", name.getCanonicalName());
    }

    @Test
    public void testFullForm() {
        ObjectName name = ObjectNameFactory.getObjectName("foo:name=value");
        assertEquals("foo:name=value", name.getCanonicalName());
    }

    @Test
    public void testShortName() {
        ObjectName name = ObjectNameFactory.getObjectName("foo:name=value,type=service,info=metric");
        String shortName = ObjectNameFactory.formatShortName(name);
        assertEquals("value-metric", shortName);
    }

}
