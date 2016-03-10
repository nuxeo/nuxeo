/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 * $Id$
 */

package org.nuxeo.ecm.directory;

import java.util.List;

import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class DirectoryFactoryProxy implements DirectoryFactory {

    private DirectoryFactory factory;

    private final String componentName;

    public DirectoryFactoryProxy(String componentName) {
        this.componentName = componentName;
    }

    /**
     * Returns the name of the component that's going to be lookup up, as it's the one knowing about its own registered
     * directories.
     *
     * @since 5.6
     */
    public String getComponentName() {
        return componentName;
    }

    private DirectoryFactory getRealObject() {
        if (factory == null) {
            factory = (DirectoryFactory) Framework.getRuntime().getComponent(componentName);
        }
        if (factory == null) {
            throw new RuntimeException(String.format("No Runtime component found for directory " + "factory '%s'",
                    componentName));
        }
        return factory;
    }

    @Override
    public Directory getDirectory(String name) throws DirectoryException {
        return getRealObject().getDirectory(name);
    }

    @Override
    public String getName() {
        return getRealObject().getName();
    }

    @Override
    public void shutdown() throws DirectoryException {
        if (factory != null) {
            factory.shutdown();
            factory = null;
        }
    }

    @Override
    public List<Directory> getDirectories() throws DirectoryException {
        return getRealObject().getDirectories();
    }

}
