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

package org.nuxeo.runtime.jboss.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Hashtable;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.naming.OperationNotSupportedException;

import org.jboss.system.ServiceDynamicMBeanSupport;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.TextTemplate;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.jboss.util.MBeanDescriptor;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RegistrationInfo;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ComponentMBean extends ServiceDynamicMBeanSupport {

    private static String serviceTemplate;

    private final RegistrationInfo ri;
    private final MBeanDescriptor mbeanDescriptor;


    public ComponentMBean(String name) throws IntrospectionException {
        ri = Framework.getRuntime().getComponentManager().getRegistrationInfo(
                new ComponentName(name));
        String desc = ri.getName().toString(); //TODO  +" version "+ri.getVersion();
        mbeanDescriptor = new MBeanDescriptor(ri.getComponent(), desc, ComponentInstance.class);
    }

    @Override
    protected Object getInternalAttribute(String attribute)
            throws AttributeNotFoundException, MBeanException, ReflectionException {
        Method getter = mbeanDescriptor.getAttributeGetter(attribute);
        if (getter == null) {
            throw new AttributeNotFoundException("MBean attribute not found: " + attribute);
        }
        try {
            return getter.invoke(ri, (Object[]) null);
        } catch (Exception e) {
            throw new MBeanException(e, "failed to invoke getter for attribute: " + attribute);
        }
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        return mbeanDescriptor.getInfo();
    }

    @Override
    protected Object internalInvoke(String actionName, Object[] params, String[] signature)
            throws MBeanException, ReflectionException {
        Method meth = mbeanDescriptor.getOperationMethod(actionName);
        if (meth == null) {
            throw new MBeanException(new OperationNotSupportedException(actionName));
        }
        try {
            return meth.invoke(ri, params);
        } catch (Exception e) {
            throw new MBeanException(e, "failed to invoke MBean operation: " + actionName);
        }
    }

    @Override
    protected void setInternalAttribute(Attribute attribute)
            throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        Method setter = mbeanDescriptor.getAttributeSetter(attribute.getName());
        if (setter == null) {
            throw new AttributeNotFoundException("MBean attribute not found: " + attribute.getName());
        }
        try {
            setter.invoke(ri, attribute.getValue());
        } catch (Exception e) {
            throw new MBeanException(e, "failed to invoke setter for attribute: " + attribute.getName());
        }
    }

    /**
     * Loads the template file for the jboss service
     * and generates the service xml definition
     * for the given service name.
     *
     * @param name the service name
     * @return the service xml definition
     */
    public static String getMBeanXMLContent(String name) {
        TextTemplate tt = new TextTemplate();
        tt.setVariable("jmxService", ComponentMBean.class.getName());
        tt.setVariable("jmxName", "nx:name=" + name + ",type=component");
        tt.setVariable("serviceName", name);
        if (serviceTemplate == null) {
            InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("org/nuxeo/runtime/jboss/adapter/component-mbean-template.xml");
            assert in != null;
            try {
                serviceTemplate = FileUtils.read(in);
            } catch (Exception e) {
                // do nothing
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
            assert serviceTemplate != null;
        }
        return tt.process(serviceTemplate);
    }

    public static ObjectName getObjectName(ComponentInstance service)
            throws MalformedObjectNameException {
        Hashtable<String, String> map = new Hashtable<String, String>();
        map.put("type", "component");
        map.put("component-type", service.getName().getType());
        map.put("name", service.getName().getName());
        return new ObjectName("nx", map);
    }


    public static String getMBeanXMLContent(ComponentName name) {
        TextTemplate tt = new TextTemplate();
        tt.setVariable("jmxService", ComponentMBean.class.getName());
        tt.setVariable("jmxName", "nx:name=" + name.getName()
                + ",component-type=" + name.getType() + ",type=component");
        tt.setVariable("serviceName", name.toString());
        if (serviceTemplate == null) {
            InputStream in = ComponentMBean.class.getClassLoader()
                .getResourceAsStream("org/nuxeo/runtime/jboss/adapter/component-mbean-template.xml");
            assert in != null;
            try {
                serviceTemplate = FileUtils.read(in);
            } catch (Exception e) {
                // do nothing
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
            assert serviceTemplate != null;
        }
        return tt.process(serviceTemplate);
    }

}
