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

package org.nuxeo.ecm.webengine.loader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.classgen.Verifier;
import org.nuxeo.ecm.webengine.WebException;

import groovy.lang.GroovyClassLoader;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class GroovyClassProxy implements ClassProxy {

    private static final Log log = LogFactory.getLog(GroovyClassLoader.class);

    protected final GroovyClassLoader loader;

    protected final String className;

    protected long timestamp = 0;

    public GroovyClassProxy(GroovyClassLoader loader, String className) {
        this.loader = loader;
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public Class<?> get() {
        try {
            Class<?> clazz = loader.loadClass(className, true, false);
            long tm = Verifier.getTimestamp(clazz);
            if (timestamp > 0 && timestamp < tm) {
                if (log.isDebugEnabled()) {
                    log.debug("CLASS CHANGED: " + clazz.getName());
                }
            }
            timestamp = tm;
            return clazz;
        } catch (ReflectiveOperationException e) {
            throw WebException.wrap(e);
        }
    }

    @Override
    public String toString() {
        return className;
    }

}
