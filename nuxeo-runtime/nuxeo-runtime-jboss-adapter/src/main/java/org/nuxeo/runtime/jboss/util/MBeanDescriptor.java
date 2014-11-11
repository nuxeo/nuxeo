/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.runtime.jboss.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class MBeanDescriptor {

    private final Map<String, Attribute> attributes;
    private final Map<String, Method> operations;

    private MBeanInfo info;
    private Class managementInterface;

    public MBeanDescriptor(Object service, String desc, Class mgmItf)
            throws IntrospectionException {
        attributes = new HashMap<String, Attribute>();
        operations = new HashMap<String, Method>();
        initialize(service, desc, mgmItf);
    }

    protected void initialize(Object service, String desc, Class mgmItf)
            throws IntrospectionException {
        if (info != null) {
            return;
        }
        managementInterface = findManagementInterface(service.getClass());
        if (managementInterface == null) {
            managementInterface = mgmItf;
        }
        MBeanAttributeInfo[] ai = findAttributes(managementInterface);
        MBeanOperationInfo[] oi = findOperations(managementInterface);

        info = new MBeanInfo(managementInterface.getName(), desc,
                ai, null, oi, null);
    }

    public MBeanInfo getInfo() {
        return info;
    }

    public Method getAttributeSetter(String name) {
        Attribute attr = attributes.get(name);
        return attr == null ? null : attr.setter;
    }

    public Method getAttributeGetter(String name) {
        Attribute attr = attributes.get(name);
        return attr == null ? null : attr.getter;
    }

    public Method getOperationMethod(String name) {
        return operations.get(name);
    }

    protected MBeanAttributeInfo[] findAttributes(Class klass) throws IntrospectionException {

        Method[] methods = klass.getMethods();

        for (Method method : methods) {
            String name = method.getName();
            if (name.startsWith("get") && method.getParameterTypes().length == 0) {
                String attrName = name.substring(3);
                Attribute ad = attributes.get(attrName);
                if (ad == null) {
                    ad = new Attribute();
                    ad.name = attrName;
                    attributes.put(attrName, ad);
                }
                ad.getter = method;
            } else if (name.startsWith("set") && method.getParameterTypes().length == 1) {
                String attrName = name.substring(3);
                Attribute ad = attributes.get(attrName);
                if (ad == null) {
                    ad = new Attribute();
                    ad.name = attrName;
                    attributes.put(attrName, ad);
                }
                ad.setter = method;
            } else if (name.startsWith("is") && method.getParameterTypes().length == 0) {
                String attrName = name.substring(2);
                Attribute ad = attributes.get(attrName);
                if (ad == null) {
                    ad = new Attribute();
                    ad.name = attrName;
                    attributes.put(attrName, ad);
                }
                ad.getter = method;
            }
        }

        Collection<Attribute> attrsDesc = attributes.values();
        int size = attrsDesc.size();
        if (size == 0) {
            return null;
        }
        MBeanAttributeInfo[] attrs = new MBeanAttributeInfo[size];
        int i = 0;
        for (Attribute ad : attrsDesc) {
            attrs[i++] = new MBeanAttributeInfo(ad.name, "", ad.getter, ad.setter);
        }
        return attrs;
    }

    protected MBeanOperationInfo[] findOperations(Class klass) {
        List<MBeanOperationInfo> list = new ArrayList<MBeanOperationInfo>();
        Method[] methods = klass.getMethods();

        for (Method method : methods) {
            String name = method.getName();
            if (!name.startsWith("get") && !name.startsWith("set")
                    && !name.startsWith("is")) {
                list.add(new MBeanOperationInfo(name, method));
                operations.put(name, method);
            }
        }
        return list.toArray(new MBeanOperationInfo[list.size()]);
    }


    public Class findManagementInterface(Class klass) {
        return _findManagementInterface(klass, klass.getName() + "MBean");
    }

    private Class _findManagementInterface(Class klass, String itfName) {
        Class[] itfs = klass.getInterfaces();
        for (Class itf : itfs) {
            if (itf.getName().endsWith(itfName)) {
                return itf;
            }
        }
        Class superKlass = klass.getSuperclass();
        if (superKlass != null) {
            return _findManagementInterface(superKlass, itfName);
        }
        return null;
    }

    static class Attribute {
        public String name;
        public Method setter;
        public Method getter;
    }

}
