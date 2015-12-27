/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.scripting;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Functions {

    private static volatile Object fn = new CoreFunctions();

    private Functions() {
    }

    public static void setInstance(Object fn) {
        if (fn == null) {
            fn = new CoreFunctions();
        }
        synchronized (Functions.class) {
            Functions.fn = fn;
        }
    }

    public static Object getInstance() {
        Object o = fn;
        if (o == null) {
            synchronized (Functions.class) {
                o = new CoreFunctions();
                fn = o;
            }
        }
        return o;
    }

}
