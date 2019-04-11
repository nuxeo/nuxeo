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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
 */
public class ModelMBeanIntrospector {

    protected final Class<?> clazz;

    protected ModelMBeanInfo managementInfo;

    protected final Map<String, ModelMBeanAttributeInfo> attributesInfo = new HashMap<>();

    protected final Map<String, ModelMBeanConstructorInfo> constructorsInfo = new HashMap<>();

    protected final Map<String, ModelMBeanOperationInfo> operationsInfo = new HashMap<>();

    protected final Map<String, ModelMBeanNotificationInfo> notificationsInfo = new HashMap<>();

    public ModelMBeanIntrospector(Class<?> clazz) {
        this.clazz = clazz;
    }

    ModelMBeanInfo introspect() {
        if (managementInfo != null) {
            return managementInfo;
        }

        // Collect ifaces
        Set<Class<?>> ifaces = new HashSet<>(1);
        if (clazz.isInterface()) {
            ifaces.add(clazz);
        } else {
            doCollectMgmtIfaces(ifaces, clazz);
            if (ifaces.isEmpty()) {
                doCollectIfaces(ifaces, clazz);
            }
        }

        // Introspect
        for (Class<?> iface : ifaces) {
            BeanInfo beanInfo;
            try {
                beanInfo = Introspector.getBeanInfo(iface);
            } catch (java.beans.IntrospectionException e) {
                throw ManagementRuntimeException.wrap("Cannot introspect " + iface, e);
            }
            doCollectAttributes(iface, beanInfo);
            doCollectConstructors(iface, beanInfo);
            doCollectOperations(iface, beanInfo);
            doCollectNotifications(iface, beanInfo);
        }

        // Assemble model mbean infos
        managementInfo = new ModelMBeanInfoSupport(clazz.getCanonicalName(), "", attributesInfo.values().toArray(
                new ModelMBeanAttributeInfo[attributesInfo.size()]), constructorsInfo.values().toArray(
                new ModelMBeanConstructorInfo[constructorsInfo.size()]), operationsInfo.values().toArray(
                new ModelMBeanOperationInfo[operationsInfo.size()]), notificationsInfo.values().toArray(
                new ModelMBeanNotificationInfo[notificationsInfo.size()]));

        return managementInfo;
    }

    protected void doCollectMgmtIfaces(Set<Class<?>> ifaces, Class<?> clazz) {
        if (clazz == null) {
            return;
        }
        if (Object.class.equals(clazz)) {
            return;
        }
        for (Class<?> iface : clazz.getInterfaces()) {
            if (iface.getName().endsWith("MBean") || iface.getName().endsWith("MXBean")) {
                ifaces.add(iface);
                doCollectMgmtIfaces(ifaces, iface);
            }
        }
        doCollectMgmtIfaces(ifaces, clazz.getSuperclass());
    }

    protected void doCollectIfaces(Set<Class<?>> ifaces, Class<?> clazz) {
        if (clazz == null) {
            return;
        }
        if (Object.class.equals(clazz)) {
            return;
        }
        for (Class<?> iface : clazz.getInterfaces()) {
            if (iface.getName().endsWith("MBean") || iface.getName().endsWith("MXBean")) {
                ifaces.clear();
                ifaces.add(iface);
                return;
            }
            ifaces.add(iface);
        }
        doCollectIfaces(ifaces, clazz.getSuperclass());
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
                Descriptor descriptor = doGetDescriptor(propertyInfo, "attribute");
                Method readMethod = propertyInfo.getReadMethod();
                Method writeMethod = propertyInfo.getWriteMethod();
                if (readMethod != null) {
                    descriptor.setField("getMethod", readMethod.getName());
                }
                if (writeMethod != null) {
                    descriptor.setField("setMethod", writeMethod.getName());
                }
                attributeInfo = new ModelMBeanAttributeInfo(propertyInfo.getName(), propertyInfo.getShortDescription(),
                        propertyInfo.getReadMethod(), propertyInfo.getWriteMethod(), descriptor);

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
                    || (name.startsWith("is") && !hasParameters && boolean.class.equals(returnType))) {
                descriptor.setField("role", "getter");
            } else if (methodInfo.getName().startsWith("set") && void.class.equals(returnType) && hasParameters
                    && parameters.length == 1) {
                // doFixAttribute(clazz, methodInfo.getName());
                descriptor.setField("role", "setter");
            } else {
                descriptor.setField("role", "operation");
            }
            ModelMBeanOperationInfo operationInfo = new ModelMBeanOperationInfo(methodInfo.getShortDescription(),
                    methodInfo.getMethod(), descriptor);
            operationsInfo.put(operationInfo.getName(), operationInfo);
        }
    }

    protected Descriptor doGetDescriptor(FeatureDescriptor info, String descriptorType) {
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
            throw new IllegalArgumentException(operationName + " does not match");
        }
        return matcher.group(2);
    }

}
