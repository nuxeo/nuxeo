/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.runtime.mockito;

import java.lang.reflect.Field;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.configuration.FieldAnnotationProcessor;

/**
 *
 *
 * @since 5.7.8
 */
public class NuxeoServiceMockAnnotationProcessor implements
        FieldAnnotationProcessor<Mock> {

    @Override
    public Object process(Mock annotation, final Field field) {

        Object mock = Mockito.mock(field.getType(), field.getName());

        if (field.isAnnotationPresent(RuntimeService.class)) {
            bindMockAsNuxeoService(field, mock);
        }

        return mock;
    }

    /**
     * @param field
     * @param mock
     *
     */
    private void bindMockAsNuxeoService(final Field field, Object mock) {
        MockProvider.INSTANCE.bind(field.getType(), mock);
    }

}
