/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.runtime.tomcat;

import java.io.File;
import java.lang.reflect.Method;

import org.apache.catalina.Container;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.WebappLoader;

/**
 * Shared attribute is experimental. Do not use it yet.
 * <p>
 * (Its purpose is to be able to deploy multiple WARs using the same nuxeo instance but it is not working yet).
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class NuxeoWebappLoader extends WebappLoader {

    protected File baseDir; // the baseDir from the Context (which is private..)

    protected void overwriteWar() {
        // File baseDir = getBaseDir();
        // remove all files
    }

    public File getBaseDir() throws ReflectiveOperationException {
        if (baseDir == null) {
            Method m = getClass().getSuperclass().getDeclaredMethod("getContainer");
            m.setAccessible(true);
            Container container = (Container) m.invoke(this);
            Method method = StandardContext.class.getDeclaredMethod("getBasePath");
            method.setAccessible(true);
            String path = (String) method.invoke(container);
            baseDir = new File(path);
        }
        return baseDir;
    }

}
