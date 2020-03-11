/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.runtime.mockito;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.configuration.FieldAnnotationProcessor;
import org.nuxeo.runtime.api.DefaultServiceProvider;

/**
 * @since 5.7.8
 */
public class NuxeoServiceMockAnnotationProcessor implements FieldAnnotationProcessor<Mock> {

    @Override
    public Object process(Mock annotation, final Field field) {

        Object mock = Mockito.mock(field.getType(), field.getName());

        for (Annotation ann : field.getAnnotations()) {
            if (ann.annotationType().equals(RuntimeService.class)) {
                bindMockAsNuxeoService(field, mock);
            }
        }

        return mock;
    }

    protected void bindMockAsNuxeoService(final Field field, Object mock) {
        MockProvider provider = (MockProvider) DefaultServiceProvider.getProvider();
        provider.bind(field.getType(), mock);
    }

}
