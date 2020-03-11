/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *    Stephane Lacoin (Nuxeo EP Software Engineer)ne Lacoin (Nuxeo EP Software Engineer)
 */
package org.nuxeo.runtime.management;

import static org.junit.Assert.assertEquals;

import javax.management.MBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfo;

import org.junit.Test;
import org.nuxeo.runtime.management.inspector.ModelMBeanInfoFactory;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public class TestMBeanInfoFactory {

    protected final ModelMBeanInfoFactory factoryUnderTest = new ModelMBeanInfoFactory();

    @Test
    public void testInterfaceProperty() {
        ModelMBeanInfo ifaceInfo = factoryUnderTest.getModelMBeanInfo(DummyMBean.class);
        MBeanAttributeInfo ifaceAttribute = ifaceInfo.getAttributes()[0];
        assertEquals("managedMessage", ifaceAttribute.getName());

        ModelMBeanInfo classInfo = factoryUnderTest.getModelMBeanInfo(DummyService.class);
        MBeanAttributeInfo classAttribute = classInfo.getAttributes()[0];
        assertEquals("managedMessage", classAttribute.getName());
    }

}
