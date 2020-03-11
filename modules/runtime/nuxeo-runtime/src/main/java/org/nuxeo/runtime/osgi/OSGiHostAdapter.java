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
 *
 * $Id$
 */

package org.nuxeo.runtime.osgi;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class OSGiHostAdapter {

    private static OSGiHostAdapter instance;

    public static void setInstance(OSGiHostAdapter instance) {
        OSGiHostAdapter.instance = instance;
    }

    public static OSGiHostAdapter getInstance() {
        return instance;
    }

    public abstract Object invoke(Object... args);

    public abstract Object getProperty(String key);

    public abstract Object getProperty(String key, Object defValue);

    public abstract void setProperty(String key, Object value);

}
