/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.common.xmap;

/**
 * Helper for class instantiation with a simple constructor with no arguments.
 *
 * @since 11.5
 */
public class XClass {

    private final Context ctx;

    private final String className;

    public XClass(Context ctx, String className) throws NoClassDefFoundError, ReflectiveOperationException {
        this.ctx = ctx;
        this.className = className;
        // sanity check
        newInstance();
    }

    public String getClassName() {
        return className;
    }

    public Class<?> getKlass() throws ClassNotFoundException {
        if (className == null) {
            return null;
        }
        return ctx.loadClass(className);
    }

    public Object newInstance() throws ReflectiveOperationException, NoClassDefFoundError {
        Class<?> klass = getKlass();
        if (klass == null) {
            return null;
        }
        return klass.getDeclaredConstructor().newInstance();
    }

}
