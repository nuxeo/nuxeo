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
package org.nuxeo.runtime.management.inspector;

import java.beans.BeanInfo;
import java.beans.FeatureDescriptor;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.ParameterDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.Descriptor;
import javax.management.IntrospectionException;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanConstructorInfo;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanNotificationInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import org.nuxeo.runtime.management.ManagementRuntimeException;

/**
 * @author matic
 *
 */
public class ModelMBeanIntrospector {

    protected final Class<?> clazz;

    protected ModelMBeanInfo managementInfo;

    protected final Map<String, ModelMBeanAttributeInfo> attributesInfo
            = new HashMap<String, ModelMBeanAttributeInfo>();

    protected final Map<String, ModelMBeanConstructorInfo> constructorsInfo
            = new HashMap<String, ModelMBeanConstructorInfo>();

    protected final Map<String, ModelMBeanOperationInfo> operationsInfo
            = new HashMap<String, ModelMBeanOperationInfo>();

    protected final Map<String, ModelMBeanNotificationInfo> notificationsInfo
            = new HashMap<String, ModelMBeanNotificationInfo>();

    public ModelMBeanIntrospector(Class<?> clazz) {
        this.clazz = clazz;
    }

    ModelMBeanInfo introspect() {
        if (managementInfo != null) {
            return managementInfo;
        }

        // Collect ifaces
        List<Class> ifaces = new ArrayList<Class>(1);
        if (clazz.isInterface()) {
            ifaces.add(clazz);
        } else {
            doCollectIfaces(ifaces, clazz);
        }

        // Introspect
        for (Class iface : ifaces) {
            BeanInfo beanInfo;
            try {
                beanInfo = Introspector.getBeanInfo(iface);
            } catch (java.beans.IntrospectionException e) {
                throw ManagementRuntimeException.wrap("Cannot introspect "
                        + iface, e);
            }
            doCollectAttributes(iface, beanInfo);
            doCollectConstructors(iface, beanInfo);
            doCollectOperations(iface, beanInfo);
            doCollectNotifications(iface, beanInfo);
        }

        // Assemble model mbean infos
        managementInfo = new ModelMBeanInfoSupport(
                clazz.getCanonicalName(),
                "",
                attributesInfo.values().toArray(
                        new ModelMBeanAttributeInfo[attributesInfo.size()]),
                constructorsInfo.values().toArray(
                        new ModelMBeanConstructorInfo[constructorsInfo.size()]),
                operationsInfo.values().toArray(
                        new ModelMBeanOperationInfo[operationsInfo.size()]),
                notificationsInfo.values().toArray(
                        new ModelMBeanNotificationInfo[notificationsInfo.size()]));

        return managementInfo;
    }

    protected void doCollectIfaces(List<Class> ifaces, Class clazz) {
        for (Class<?> iface : clazz.getInterfaces()) {
            if (iface.getName().endsWith("MBean")) {
                ifaces.clear();
                ifaces.add(iface);
                return;
            }
            ifaces.add(iface);
        }
    }

    protected void doCollectNotifications(Class<?> clazz, BeanInfo info) {
    }

    protected void doCollectAttributes(Class<?> clazz, BeanInfo beanInfo) {
        for (PropertyDescriptor propertyInfo : beanInfo.getPropertyDescriptors()) {
            if (propertyInfo.isHidden()) {
                continue;
            }
            ModelMBeanAttributeInfo attributeInfo = null;
            try {
                Descriptor descriptor =
                    doGetDescriptor(propertyInfo, "attribute");
                Method readMethod = propertyInfo.getReadMethod();
                Method writeMethod = propertyInfo.getWriteMethod();
                if (readMethod != null) {
                    descriptor.setField("getMethod", readMethod.getName());
                }
                if (writeMethod != null) {
                    descriptor.setField("setMethod", writeMethod.getName());
                }
                attributeInfo = new ModelMBeanAttributeInfo(
                        propertyInfo.getName(),
                        propertyInfo.getShortDescription(),
                        propertyInfo.getReadMethod(),
                        propertyInfo.getWriteMethod(),
                        descriptor);

            } catch (IntrospectionException e) {
                continue;
            }
            attributesInfo.put(attributeInfo.getName(), attributeInfo);
        }
    }

    protected void doCollectConstructors(Class<?> clazz, BeanInfo info) {
    }

    protected void doCollectOperations(Class<?> clazz, BeanInfo beanInfo) {
        for (MethodDescriptor methodInfo : beanInfo.getMethodDescriptors()) {
            if (methodInfo.isHidden()) {
                continue;
            }
            Descriptor descriptor = doGetDescriptor(methodInfo, "operation");
            String name = methodInfo.getName();
            Method method = methodInfo.getMethod();
            ParameterDescriptor[] parameters = methodInfo.getParameterDescriptors();
            boolean hasParameters = parameters != null && parameters.length > 0;
            Class<?> returnType = method.getReturnType();
            boolean returnValue = returnType != null && !void.class.equals(returnType);
            if ((name.startsWith("get") && hasParameters && returnValue)
                    || (name.startsWith("is")
                            && !hasParameters && boolean.class.equals(returnType))) {
                descriptor.setField("role", "getter");
            }  else if (methodInfo.getName().startsWith("set")
                    && void.class.equals(returnType)
                    && hasParameters && parameters.length == 1) {
//                doFixAttribute(clazz, methodInfo.getName());
                descriptor.setField("role", "setter");
            } else {
                descriptor.setField("role", "operation");
            }
            ModelMBeanOperationInfo operationInfo = new ModelMBeanOperationInfo(
                    methodInfo.getShortDescription(), methodInfo.getMethod(), descriptor);
            operationsInfo.put(operationInfo.getName(), operationInfo);
        }
    }

    protected Descriptor doGetDescriptor(FeatureDescriptor info,
            String descriptorType) {
        Descriptor descriptor = new DescriptorSupport();
        descriptor.setField("name", info.getName());
        descriptor.setField("displayName", info.getDisplayName());
        descriptor.setField("description", info.getShortDescription());
        descriptor.setField("descriptorType", descriptorType);
        return descriptor;
    }

    private final Pattern attributePattern = Pattern.compile("(get|set|is)(.*)");

    protected String doExtractMethodSuffix(String operationName) {
        Matcher matcher = attributePattern.matcher(operationName);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(operationName
                    + " does not match");
        }
        return matcher.group(2);
    }

//    protected void doFixAttribute(Class<?> inspected, String operationName)
//            throws Exception {
//        String methodSuffix = doExtractMethodSuffix(operationName);
//        String attributeName = methodSuffix.substring(0, 1).toLowerCase()
//                + methodSuffix.substring(1);
//
//        if (attributesInfo.containsKey(attributeName)) {
//            return;
//        }
//
//        Method reader = null;
//        Method writter = null;
//        for (Method method : inspected.getMethods()) {
//            Matcher matcher = attributePattern.matcher(method.getName());
//            if (!matcher.matches()) {
//                continue;
//            }
//            if (!matcher.group(2).equals(methodSuffix)) {
//                continue;
//            }
//            String prefix = matcher.group(1);
//            if (prefix.equals("is")) {
//                if (reader == null) {
//                    reader = method;
//                }
//            } else if (prefix.equals("get")) {
//                reader = method;
//            } else if (prefix.equals("set")) {
//                writter = method;
//            }
//        }
//
//         Descriptor descriptor = getDescriptor(null, null, attributeName,
//         null,
//         null, "attribute");
//         if (reader != null) {
//         descriptor.setField("getMethod", reader.getName());
//         }
//         if (writter != null) {
//         descriptor.setField("setMethod", writter.getName());
//         }
//
//
//
//         attributesInfo.put(attributeName, new ModelMBeanAttributeInfo(
//         attributeName, attributeName, reader, writter));
//    }
}
