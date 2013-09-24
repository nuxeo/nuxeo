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
package org.mockito.configuration;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.runtime.model.DefaultComponent;

/**
 *
 *
 * @since 5.7.8
 */
public class MockProvider extends DefaultComponent {

    private static Map<Class<?>, Object> mocks = new HashMap<>();

    /**
     * @param type
     * @param mock
     *
     */
    public static void bind(Class<?> klass, Object mock) {
        mocks.put(klass, mock);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (mocks.containsKey(adapter)) {
            return (T) mocks.get(adapter);
        }
        return null;
    }

}
