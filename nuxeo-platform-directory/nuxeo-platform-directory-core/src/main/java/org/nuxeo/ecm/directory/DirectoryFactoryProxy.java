/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 * $Id$
 */

package org.nuxeo.ecm.directory;

import java.util.List;

import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
public class DirectoryFactoryProxy implements DirectoryFactory {

    private DirectoryFactory factory;

    private final String componentName;

    public DirectoryFactoryProxy(String componentName) {
        this.componentName = componentName;
    }

    private DirectoryFactory getRealObject() {
        if (factory == null) {
            factory = (DirectoryFactory) Framework.getRuntime().getComponent(componentName);
        }
        return factory;
    }

    public Directory getDirectory(String name) throws DirectoryException {
        return getRealObject().getDirectory(name);
    }

    public String getName() {
        return getRealObject().getName();
    }

    public void shutdown() throws DirectoryException {
        if (factory != null) {
            factory.shutdown();
            factory = null;
        }
    }

    public List<Directory> getDirectories() throws DirectoryException {
        return getRealObject().getDirectories();
    }

}
