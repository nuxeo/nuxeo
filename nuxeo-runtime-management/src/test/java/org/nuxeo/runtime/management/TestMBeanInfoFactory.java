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

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.modelmbean.ModelMBeanInfo;

import junit.framework.TestCase;

import org.nuxeo.runtime.management.inspector.ModelMBeanInfoFactory;

/**
 * @author matic
 *
 */
public class TestMBeanInfoFactory extends TestCase {

    protected ModelMBeanInfoFactory factoryUnderTest = 
        new ModelMBeanInfoFactory();
    
    public void testInterfaceProperty() throws Exception {
        ModelMBeanInfo ifaceInfo =
            factoryUnderTest.getModelMBeanInfo(DummyManagedServiceManagement.class);
        MBeanAttributeInfo ifaceAttribute = ifaceInfo.getAttributes()[0];
        assertEquals("managedMessage", ifaceAttribute.getName());
        ModelMBeanInfo classInfo =
            factoryUnderTest.getModelMBeanInfo(DummyManagedServiceImpl.class);
        MBeanAttributeInfo classAttribute = classInfo.getAttributes()[0];
        assertEquals("message", classAttribute.getName());
    }
}
