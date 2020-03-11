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
package org.nuxeo.runtime.tomcat.dev;

import java.lang.reflect.Method;

/**
 * Invokes the Framework by reflection as this module does not have access to the runtime context.
 *
 * @since 9.3
 */
public class FrameworkInvoker {

    protected final Method isBooleanPropertyTrue;

    public FrameworkInvoker(ClassLoader cl) throws ReflectiveOperationException {
        Class<?> frameworkClass = cl.loadClass("org.nuxeo.runtime.api.Framework");
        isBooleanPropertyTrue = frameworkClass.getDeclaredMethod("isBooleanPropertyTrue", String.class);
    }

    public boolean isBooleanPropertyTrue(String propName) throws ReflectiveOperationException {
        return ((Boolean) isBooleanPropertyTrue.invoke(null, propName)).booleanValue();
    }

}
