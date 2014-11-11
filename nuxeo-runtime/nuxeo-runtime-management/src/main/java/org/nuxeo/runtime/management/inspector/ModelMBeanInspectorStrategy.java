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
 *     Stephane Lacoin (Nuxeo EP Software Engineer)
 */
package org.nuxeo.runtime.management.inspector;

import java.beans.BeanInfo;
import java.beans.FeatureDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.ParameterDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.Descriptor;
import javax.management.MBeanException;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanConstructorInfo;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanNotificationInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;
import javax.management.modelmbean.XMLParseException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsesoft.mmbi.JMX;
import org.jsesoft.mmbi.JMXNotification;
import org.jsesoft.ri.InspectorSupport;

public class ModelMBeanInspectorStrategy extends InspectorSupport {

    // ESCA-JAVA0242:
    private static final transient Log log = LogFactory.getLog(ModelMBeanInspectorStrategy.class);

    /** The mbean's descriptor. */
    private Descriptor mbeanDescriptor;

    /** The mbean's ModelMBeanInfo. */
    private ModelMBeanInfo mbeanInfo;

    /** The actually inspected classes JavaBean BeanInfo */
    private BeanInfo beanInfo;

    /** The mbean's constructor infos. */
    private Vector<ModelMBeanConstructorInfo> constructorInfos = new Vector<ModelMBeanConstructorInfo>();

    /** The mbean's operation infos. */
    private Vector<ModelMBeanOperationInfo> operationInfos = new Vector<ModelMBeanOperationInfo>();

    /** The mbean's attribute infos. */
    private Vector<ModelMBeanAttributeInfo> attributeInfos = new Vector<ModelMBeanAttributeInfo>();

    /** The mbean's notification infos. */
    private Vector<ModelMBeanNotificationInfo> notificationInfos = new Vector<ModelMBeanNotificationInfo>();

    /** A type specifier for Vector.toArray() */
    private final ModelMBeanConstructorInfo[] constructors = new ModelMBeanConstructorInfo[0];

    /** A type specifier for Vector.toArray() */
    private final ModelMBeanOperationInfo[] operations = new ModelMBeanOperationInfo[0];

    /** A type specifier for Vector.toArray() */
    private final ModelMBeanAttributeInfo[] attributes = new ModelMBeanAttributeInfo[0];

    /** A type specifier for Vector.toArray() */
    private final ModelMBeanNotificationInfo[] notifications = new ModelMBeanNotificationInfo[0];

    private final Class<?> sentinel;

    public ModelMBeanInspectorStrategy(Class<?> inspected)
            throws IntrospectionException {
        this.sentinel = inspected.isInterface() ? null : Object.class;
        this.beanInfo = Introspector.getBeanInfo(inspected);
    }

    /**
     * This override creates a JMX descriptor for the inspected class.
     * 
     * <p>
     * <b>Note:</b> This function is not intended to be called by customers.
     * Instead, it is called by the inspector.
     * </p>
     * 
     * @param inspected the inspected class
     * @throws Exception if inspection fails
     * @return true if inspection complete
     */
    @Override
    public boolean inspect(Class<?> inspected) throws Exception {
        if (inspected.equals(sentinel)) {
            return true;
        }
        BeanInfo oldBeanInfo = beanInfo;
        beanInfo = Introspector.getBeanInfo(inspected, sentinel);
        handleJMXNotificationAnnotations(inspected.getAnnotations());
        JMX annotation = inspected.getAnnotation(org.jsesoft.mmbi.JMX.class);

        Descriptor descriptor = getDescriptor(annotation,
                beanInfo.getBeanDescriptor(), inspected.getName(),
                inspected.getSimpleName(), inspected.getCanonicalName(),
                "MBean");
        mbeanDescriptor = descriptor;
        if (log.isDebugEnabled()) {
            log.debug(((DescriptorSupport) descriptor).toXMLString());
        }
        beanInfo = oldBeanInfo;
        return false;
    }

    /**
     * This override creates a constructor info for the inspected constructor.
     * 
     * <p>
     * <b>Note:</b> This function is not intended to be called by customers.
     * Instead, it is called by the inspector.
     * </p>
     * 
     * @param inspected the inspected class
     * @param constructor the inspected constructor
     * @throws Exception if inspection fails
     * @return true if inspection complete
     */
    @Override
    public boolean inspectConstructor(Class<?> inspected,
            Constructor<?> constructor) throws Exception {
        if (!Modifier.isPublic(constructor.getModifiers())) {
            return false;
        }
        handleJMXNotificationAnnotations(constructor.getAnnotations());
        JMX annotation = constructor.getAnnotation(org.jsesoft.mmbi.JMX.class);
        if ((annotation != null) && (annotation.hide())) {
            return false;
        }
        MBeanParameterInfo[] parameterInfos = getParameterInfos(
                constructor.getParameterTypes(),
                constructor.getParameterAnnotations(), null);
        String name = constructor.getName();
        Descriptor descriptor = getDescriptor(annotation, null, name, name,
                name, "operation");
        descriptor.setField("role", "constructor");
        constructorInfos.add(new ModelMBeanConstructorInfo(
                (String) descriptor.getFieldValue("name"),
                (String) descriptor.getFieldValue("description"),
                parameterInfos, descriptor));
        if (log.isDebugEnabled()) {
            log.debug(((DescriptorSupport) descriptor).toXMLString());
        }
        return false;
    }

    /**
     * This override creates a operation info for the inspected method.
     * 
     * <p>
     * <b>Note:</b> This function is not intended to be called by customers.
     * Instead, it is called by the inspector.
     * </p>
     * 
     * @param inspected the inspected class
     * @param operation the inspected method
     * @throws Exception if inspection fails
     * @return true if inspection complete
     */
    @Override
    public boolean inspectMethod(Class<?> inspected, Method operation)
            throws Exception {
        if (!Modifier.isPublic(operation.getModifiers())) {
            return false;
        }
        handleJMXNotificationAnnotations(operation.getAnnotations());
        JMX annotation = operation.getAnnotation(org.jsesoft.mmbi.JMX.class);
        if ((annotation != null) && (annotation.hide())) {
            return false;
        }
        MethodDescriptor methodDescriptor = getMethodDescriptor(
                beanInfo.getMethodDescriptors(), operation);
        ParameterDescriptor[] parameterDescriptors = (methodDescriptor == null) ? null
                : methodDescriptor.getParameterDescriptors();
        MBeanParameterInfo[] parameterInfos = getParameterInfos(
                operation.getParameterTypes(),
                operation.getParameterAnnotations(), parameterDescriptors);
        String name = operation.getName();
        Descriptor descriptor = getDescriptor(annotation, methodDescriptor,
                name, name, name, "operation");
        descriptor.setField("class", inspected.getName());
        descriptor.setField("displayName", descriptor.getFieldValue("name"));
        if ((operation.getName().startsWith("get")
                && (operation.getParameterTypes().length == 0) && (operation.getReturnType() != null))
                || (operation.getName().startsWith("is")
                        && (operation.getParameterTypes().length == 0) && boolean.class.equals(operation.getReturnType()))) {
            doFixAttribute(inspected, operation.getName());
            descriptor.setField("role","getter");
        } else if (operation.getName().startsWith("set")
                && (void.class.equals(operation.getReturnType()))
                && (operation.getParameterTypes().length == 0)
                && (operation.getReturnType() != null)) {
            doFixAttribute(inspected, operation.getName());
            descriptor.setField("role", "setter");
        } else {
            descriptor.setField("role", "operation");
        }
        int impact = (annotation == null) ? MBeanOperationInfo.ACTION_INFO
                : annotation.impact();
        if (impact == MBeanOperationInfo.UNKNOWN) {
            impact = MBeanOperationInfo.ACTION_INFO;
        }
        operationInfos.add(new ModelMBeanOperationInfo(
                (String) descriptor.getFieldValue("name"),
                (String) descriptor.getFieldValue("description"),
                parameterInfos, operation.getReturnType().getName(), impact,
                descriptor));
        if (log.isDebugEnabled()) {
            log.debug(((DescriptorSupport) descriptor).toXMLString());
        }

        return false;
    }

    private MBeanParameterInfo[] getParameterInfos(Class<?>[] types,
            Annotation[][] annotations, ParameterDescriptor[] features) {
        if (types.length == 0) {
            return new MBeanParameterInfo[0];
        }
        MBeanParameterInfo[] parameterInfos = new MBeanParameterInfo[types.length];
        for (int iX = 0; iX < types.length; iX++) {
            Class<?> type = types[iX];
            JMX annotation = getJMXAnnotation(annotations[iX]);
            String name = (features == null) ? null : features[iX].getName();
            if (name == null) {
                name = "p" + iX;
            }
            String description = (features == null) ? null
                    : features[iX].getShortDescription();
            if (description == null) {
                description = (annotation == null) ? ""
                        : annotation.description();
            }
            if ("".equals(description)) {
                description = name;
            }
            parameterInfos[iX] = new MBeanParameterInfo(name, type.getName(),
                    description);
        }
        return parameterInfos;
    }

    /**
     * Gets the JavaBeans method descriptor for the method.
     * 
     * @param features the BeanInfo MethodDescriptors
     * @param method the Method
     * @return the MethodDescriptor, null if none
     */
    public MethodDescriptor getMethodDescriptor(MethodDescriptor[] features,
            Method method) {
        for (MethodDescriptor feature : features) {
            if (feature.getMethod().equals(method)) {
                return feature;
            }
        }
        return null;
    }

    /**
     * This override creates a attribute info for the inspected field.
     * 
     * <p>
     * <b>Note:</b> This function is not intended to be called by customers.
     * Instead, it is called by the inspector.
     * </p>
     * 
     * @param inspected the inspected class
     * @param field the inspected field
     * @throws Exception if inspection fails
     * @return true if inspection complete
     */
    @Override
    public boolean inspectField(Class<?> inspected, Field field)
            throws Exception {
        JMX annotation = field.getAnnotation(org.jsesoft.mmbi.JMX.class);
        if ((annotation != null) && (annotation.hide())) {
            return false;
        }
        PropertyDescriptor propertyDescriptor = getPropertyDescriptor(
                beanInfo.getPropertyDescriptors(), field);
        Method getter = null;
        if (propertyDescriptor != null) {
            getter = propertyDescriptor.getReadMethod();
        }
        if (getter == null) {
            getter = getGetter(inspected, field);
        }
        Method setter = null;
        if (propertyDescriptor != null) {
            setter = propertyDescriptor.getWriteMethod();
        }
        if (setter == null) {
            setter = getSetter(inspected, field);
        }
        if ((setter == null) && (getter == null)) {
            return false;
        }
        String name = field.getName();
        Descriptor descriptor = getDescriptor(annotation, propertyDescriptor,
                name, name, name, "attribute");
        if (getter != null) {
            descriptor.setField("getMethod", getter.getName());
        }
        if (setter != null) {
            descriptor.setField("setMethod", setter.getName());
        }
        attributeInfos.add(new ModelMBeanAttributeInfo(
                (String) descriptor.getFieldValue("name"),
                (String) descriptor.getFieldValue("description"), getter,
                setter, descriptor));
        if (log.isDebugEnabled()) {
            log.debug(((DescriptorSupport) descriptor).toXMLString());
        }
        return false;
    }

    /**
     * Gets the BeanInfe PropertyDescriptor for the field.
     * 
     * @param features the BeanInfo PropertyDescriptors
     * @param field the Field
     * @return the PropertyDescriptor, null if none
     */
    public PropertyDescriptor getPropertyDescriptor(
            PropertyDescriptor[] features, Field field) {
        for (PropertyDescriptor feature : features) {
            if (feature.getName().equals(field.getName())) {
                return feature;
            }
        }
        return null;
    }

    /**
     * Inquires the getter method for a field.
     * 
     * @param inspected the inspected Class
     * @param field the inspected Field
     * @return getter, null if none defined
     */
    public Method getGetter(Class<?> inspected, Field field) {
        String name = field.getName();
        String upper = Character.toUpperCase(name.charAt(0))
                + name.substring(1);
        for (Method method : inspected.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            if (boolean.class.equals(field.getType())
                    && boolean.class.equals(method.getReturnType())
                    && (method.getParameterTypes().length == 0)
                    && ("is" + upper).equals(method.getName())) {
                return method;
            }
            if (field.getType().equals(method.getReturnType())
                    && (method.getParameterTypes().length == 0)
                    && ("get" + upper).equals(method.getName())) {
                return method;
            }
        }
        return null;
    }

    /**
     * Inquires the setter method for a field.
     * 
     * @param inspected the inspected Class
     * @param field the inspected Field
     * @return setter, null if none defined
     */
    public Method getSetter(Class<?> inspected, Field field) {
        String name = field.getName();
        String upper = Character.toUpperCase(name.charAt(0))
                + name.substring(1);
        for (Method method : inspected.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            Class<?>[] types = method.getParameterTypes();
            if (types.length != 1) {
                continue;
            }
            if (field.getType().equals(types[0])
                    && ("set" + upper).equals(method.getName())) {
                return method;
            }
        }
        return null;
    }

    /**
     * Inquires the JMX annotation.
     * 
     * @param annotations the annotations to be looked up for the JMX annotation
     * @return JMX
     */
    public JMX getJMXAnnotation(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().isAssignableFrom(
                    org.jsesoft.mmbi.JMX.class)) {
                return (JMX) annotation;
            }
        }
        return null;
    }

    /**
     * Handles the JMXNotification annotations.
     * 
     * @param annotations the annotations to be looked up for the JMX annotation
     */
    public void handleJMXNotificationAnnotations(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().isAssignableFrom(
                    org.jsesoft.mmbi.JMXNotification.class)) {
                JMXNotification notification = (JMXNotification) annotation;
                Descriptor descriptor;
                descriptor = new DescriptorSupport();
                descriptor.setField("name", notification.name());
                descriptor.setField("displayName", notification.displayName());
                descriptor.setField("description", notification.description());
                descriptor.setField("severity", notification.severity());
                descriptor.setField("descriptorType", "notification");
                if (notificationInfos == null) {
                    notificationInfos = new Vector<ModelMBeanNotificationInfo>();
                }
                notificationInfos.add(new ModelMBeanNotificationInfo(
                        notification.types(), notification.name(),
                        notification.description(), descriptor));
                if (log.isDebugEnabled()) {
                    log.debug(((DescriptorSupport) descriptor).toXMLString());
                }
            }
        }
    }

    /**
     * Creates a JMX descriptor from the specified information.
     * 
     * <p>
     * Default information can be overruled by annotation information
     * </p>
     * <p>
     * <b>Note:</b> Though this was designed as a helper function, it might be
     * useful for customers, too.
     * </p>
     * 
     * @param annotation the JMX annotation containing overriding info
     * @param feature the Javabeans feature descriptor containing overriding
     *            info
     * @param defaultName the default name
     * @param defaultDisplayName the default display name
     * @param defaultDescription the default description
     * @param descriptorType the type of the descriptor to create
     * @return the created descriptor
     * @throws MBeanException if descriptor cannot be created
     * @throws XMLParseException if the annotation's XML is not valid
     */
    public Descriptor getDescriptor(JMX annotation, FeatureDescriptor feature,
            String defaultName, String defaultDisplayName,
            String defaultDescription, String descriptorType)
            throws MBeanException, XMLParseException {
        Descriptor descriptor;
        if (annotation != null) {
            String xml = annotation.xml();
            if ("".equals(xml)) {
                descriptor = new DescriptorSupport();
                descriptor.setField("name", defaultName);
            } else {
                descriptor = new DescriptorSupport(xml);
            }
        } else {
            descriptor = new DescriptorSupport();
            descriptor.setField("name", defaultName);
        }
        descriptor.setField("descriptorType", descriptorType);
        String name = (String) descriptor.getFieldValue("name");
        if ((feature != null) && (feature.getName() != null)) {
            name = feature.getName();
        }
        if ((annotation != null) && (!"".equals(annotation.name()))) {
            name = annotation.name();
        }
        String displayName = (String) descriptor.getFieldValue("displayName");
        if ((feature != null) && (feature.getDisplayName() != null)) {
            displayName = feature.getDisplayName();
        }
        if ((annotation != null) && (!"".equals(annotation.displayName()))) {
            displayName = annotation.displayName();
        }
        if ((displayName == null) || "".equals(displayName)) {
            displayName = defaultDisplayName;
        }
        String description = (String) descriptor.getFieldValue("description");
        if ((feature != null) && (feature.getShortDescription() != null)) {
            description = feature.getShortDescription();
        }
        if ((annotation != null) && (!"".equals(annotation.description()))) {
            description = annotation.description();
        }
        if ((description == null) || "".equals(description)) {
            description = defaultDescription;
        }
        descriptor.setField("name", name);
        descriptor.setField("displayName", displayName);
        descriptor.setField("description", description);
        return descriptor;
    }

    /**
     * Gets the created constructor infos.
     * 
     * @return ModelMBeanConstructorInfo[]
     */
    public ModelMBeanConstructorInfo[] getConstructorInfos() {
        if (constructorInfos == null) {
            return new ModelMBeanConstructorInfo[0];
        }
        return constructorInfos.toArray(constructors);
    }

    /**
     * Gets the created operation info.
     * 
     * @return ModelMBeanOperationInfo[]
     */
    public ModelMBeanOperationInfo[] getOperationInfos() {
        if (operationInfos == null) {
            return new ModelMBeanOperationInfo[0];
        }
        return operationInfos.toArray(operations);
    }

    /**
     * Gets the created attribute info.
     * 
     * @return ModelMBeanAttributeInfo[]
     */
    public ModelMBeanAttributeInfo[] getAttributeInfos() {
        if (attributeInfos == null) {
            return new ModelMBeanAttributeInfo[0];
        }
        return attributeInfos.toArray(attributes);
    }

    /**
     * Gets the created attribute info.
     * 
     * @return ModelMBeanAttributeInfo[]
     */
    public ModelMBeanNotificationInfo[] getNotificationInfos() {
        if (notificationInfos == null) {
            return new ModelMBeanNotificationInfo[0];
        }
        return notificationInfos.toArray(notifications);
    }

    /**
     * Gets the created MBeanInfo.
     * 
     * <p>
     * This function creates the MBean Info, if not already done.
     * </p>
     * 
     * @return ModelMBeanInfo
     */
    public ModelMBeanInfo getMBeanInfo() {

        if (mbeanInfo != null) {
            return mbeanInfo;
        }

        return mbeanInfo = new ModelMBeanInfoSupport(
                (String) mbeanDescriptor.getFieldValue("name"),
                (String) mbeanDescriptor.getFieldValue("description"),
                getAttributeInfos(), getConstructorInfos(),
                getOperationInfos(), getNotificationInfos(), mbeanDescriptor);
    }

    private static final Pattern attributePattern = Pattern.compile("(get|set|is)(.*)");

    protected String doExtractMethodSuffix(String operationName) {
        Matcher matcher = attributePattern.matcher(operationName);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(operationName
                    + " does not match");
        }
        return matcher.group(2);
    }

    protected boolean hasAttributeInfo(String attributeName) {
       for (ModelMBeanAttributeInfo attributeInfo : attributeInfos) {
            if (attributeInfo.getName().equals(attributeName)) {
                return true;
            }
        }
        return false;
    }

    protected void doFixAttribute(Class<?> inspected, String operationName)
            throws Exception {
        String methodSuffix = doExtractMethodSuffix(operationName);
        String attributeName = methodSuffix.substring(0, 1).toLowerCase()
                + methodSuffix.substring(1);

        if (hasAttributeInfo(attributeName)) {
            return;
        }
 
        Method reader = null;
        Method writter = null;
        for (Method method : inspected.getMethods()) {
            Matcher matcher = attributePattern.matcher(method.getName());
            if (!matcher.matches()) {
                continue;
            }
            if (!matcher.group(2).equals(methodSuffix)) {
                continue;
            }
            String prefix = matcher.group(1);
            if (prefix.equals("is")) {
                if (reader == null) {
                    reader = method;
                }
            } else if (prefix.equals("get")) {
                reader = method;
            } else if (prefix.equals("set")) {
                writter = method;
            }

        }

        Descriptor descriptor = getDescriptor(null, null, attributeName, null,
                null, "attribute");
        if (reader != null) {
            descriptor.setField("getMethod", reader.getName());
        }
        if (writter != null) {
            descriptor.setField("setMethod", writter.getName());
        }
        if (attributeInfos == null) {
            attributeInfos = new Vector<ModelMBeanAttributeInfo>();
        }
        attributeInfos.add(new ModelMBeanAttributeInfo(attributeName,
                attributeName, reader, writter, descriptor));

        if (log.isDebugEnabled()) {
            log.debug(((DescriptorSupport) descriptor).toXMLString());
        }
    }
}