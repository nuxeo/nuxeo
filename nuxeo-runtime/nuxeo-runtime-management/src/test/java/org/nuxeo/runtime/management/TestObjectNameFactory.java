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

import javax.management.ObjectName;

import org.nuxeo.runtime.management.ObjectNameFactory;

import junit.framework.TestCase;

/**
 * @author matic
 *
 */
public class TestObjectNameFactory extends TestCase {

    public void testSimpleForm() {
        ObjectName name = ObjectNameFactory.getObjectName("simple");
        assertEquals(name.getCanonicalName(), "nx:name=simple,type=service");
    }
    
    public void testAvaForm() {
        ObjectName name = ObjectNameFactory.getObjectName("name=value");
        assertEquals(name.getCanonicalName(), "nx:name=value");
    }
    
    
    public void testFullForm() {
        ObjectName name = ObjectNameFactory.getObjectName("foo:name=value");
        assertEquals(name.getCanonicalName(), "foo:name=value");
    }
    
    
    public void testShortName() {
        ObjectName name = ObjectNameFactory.getObjectName("foo:name=value,type=service,info=metric");
        String shortName = ObjectNameFactory.formatShortName(name);
        assertEquals("value-metric", shortName);
    }
}
