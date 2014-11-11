/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.binary;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Service holding the registered binary managers.
 * <p>
 * Actual instantiation is done by storage backends.
 *
 * @since 5.9.4
 */
public class BinaryManagerService extends DefaultComponent {

    protected Map<String, BinaryManager> registry = new ConcurrentHashMap<String, BinaryManager>();

    @Override
    public void activate(ComponentContext context) throws Exception {
        registry.clear();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        registry.clear();
    }

    public void addBinaryManager(String repositoryName,
            BinaryManager binaryManager) {
        registry.put(repositoryName, binaryManager);
    }

    public void removeBinaryManager(String repositoryName) {
        registry.remove(repositoryName);
    }

    public BinaryManager getBinaryManager(String repositoryName) {
        return registry.get(repositoryName);
    }

}
