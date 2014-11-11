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

import org.nuxeo.ecm.webengine.WebException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class LazyClassProxy implements ClassProxy {

    protected final String className;
    protected final ClassLoader loader;
    protected Class<?> clazz;

    public LazyClassProxy(ClassLoader loader, String className) {
        this.loader = loader;
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public ClassLoader getLoader() {
        return loader;
    }

    public Class<?> get() {
        if (clazz == null) {
            try {
                clazz = loader.loadClass(className);
            } catch (Exception e) {
                throw WebException.wrap("Failed to load class "+className, e);
            }
        }
        return clazz;
    }

    public void reset() {
        clazz = null;
    }

    @Override
    public String toString() {
        return className;
    }

}
