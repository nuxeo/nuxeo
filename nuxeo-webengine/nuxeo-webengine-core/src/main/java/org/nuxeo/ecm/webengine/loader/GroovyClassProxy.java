/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.loader;

import groovy.lang.GroovyClassLoader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.classgen.Verifier;
import org.nuxeo.ecm.webengine.WebException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
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
                     log.debug("CLASS CHANGED: "+clazz.getName());
                 }
             }
             timestamp = tm;
             return clazz;
         } catch (ClassNotFoundException e) {
             throw WebException.wrap("Class Not found: '" + className + "'", e);
         } catch (Exception e) {
             throw WebException.wrap(e);
         }
    }

    @Override
    public String toString() {
        return className;
    }

}
