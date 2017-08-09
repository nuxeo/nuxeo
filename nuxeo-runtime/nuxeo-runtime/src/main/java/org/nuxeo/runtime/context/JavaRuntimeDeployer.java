/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 *
 */
package org.nuxeo.runtime.context;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.nuxeo.runtime.model.RuntimeDeployer;

/**
 * TODO JAVADOC !!!
 *
 * @since 9.3
 */
public class JavaRuntimeDeployer implements RuntimeDeployer {

    protected final RuntimeService runtimeService;

    /**
     * @param runtimeService the component runtime service used to register components
     */
    public JavaRuntimeDeployer(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @Override
    public boolean accept(String component) {
        try {
            Class.forName(component, false, this.getClass().getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    // TODO JAVADOC
    @Override
    public RegistrationInfo deploy(String className) {
        try {
            // get the class
            Class<?> clazz = Class.forName(className);
            // instantiate the component
            Object component = clazz.newInstance();
            // we force to annotate component classes with Component annotation (it'll be used later for class scan)
            // don't handle inheritance for now
            Component componentAnnotation = clazz.getDeclaredAnnotation(Component.class);
            String componentName = StringUtils.defaultIfBlank(componentAnnotation.name(), componentAnnotation.value());
            componentName = StringUtils.defaultIfBlank(componentName, className);
            // build the registration info
            JavaRegistrationInfo.Builder riBuilder = new JavaRegistrationInfo.Builder(componentName);
            // look for methods annotated with @Descriptor
            for (Method method : clazz.getMethods()) {
                if (method.isAnnotationPresent(Descriptor.class)) {
                    riBuilder.add(buildExtension(component, method));
                }
            }
            JavaRegistrationInfo ri = riBuilder.build();

            // register the component
            runtimeService.getComponentManager().register(ri);
            return ri;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected DescriptorExtension buildExtension(Object component, Method method)
            throws IllegalAccessException, InvocationTargetException {
        // build the descriptor contribution
        Descriptor annotation = method.getAnnotation(Descriptor.class);
        ComponentName targetName = new ComponentName(annotation.target());
        String extensionPoint = annotation.point();

        // build the descriptor extension
        DescriptorExtension extension = new DescriptorExtension(targetName, extensionPoint);
        Class<?> returnType = method.getReturnType();
        if (Collection.class.isAssignableFrom(returnType)) {
            extension.contributions = ((Collection) method.invoke(component)).toArray();
        } else {
            extension.contributions = new Object[] { method.invoke(component) };
        }
        return extension;
    }

}
