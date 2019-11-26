/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.runtime.test.runner;

import java.util.Properties;

import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.runtime.api.Framework;

/**
 * Features handling {@link WithFrameworkProperty} annotations.
 *
 * @since 11.1
 */
public class WithFrameworkPropertyFeature implements RunnerFeature {

    protected Properties previousProperties = new Properties();

    @Override
    public void beforeSetup(FeaturesRunner runner, FrameworkMethod method, Object test) {
        Properties properties = Framework.getProperties();
        for (WithFrameworkProperty annot : method.getMethod().getAnnotationsByType(WithFrameworkProperty.class)) {
            String propertyKey = annot.name();
            Object previousProperty = properties.remove(propertyKey);
            if (previousProperty != null) {
                previousProperties.put(propertyKey, previousProperty);
            }
            properties.put(propertyKey, annot.value());
        }
    }

    @Override
    public void afterTeardown(FeaturesRunner runner, FrameworkMethod method, Object test) {
        Properties properties = Framework.getProperties();
        for (WithFrameworkProperty annot : method.getMethod().getAnnotationsByType(WithFrameworkProperty.class)) {
            String propertyKey = annot.name();
            if (previousProperties.contains(propertyKey)) {
                properties.put(propertyKey, previousProperties.get(propertyKey));
            } else {
                properties.remove(propertyKey);
            }
        }
        previousProperties.clear();
    }

}
