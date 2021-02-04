/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XMerge;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.drive.service.FileSystemItemAdapterService;

/**
 * XMap descriptor for the {@code activeFileSystemItemFactories} contributions to the
 * {@code activeFileSystemItemFactories} extension point of the {@link FileSystemItemAdapterService}.
 *
 * @author Antoine Taillefer
 */
@XObject(ActiveItemFactoryRegistry.FILE_SYSTEM_NODE_NAME)
@XRegistry(merge = false)
public class ActiveFileSystemItemFactoriesDescriptor {

    @XNode(XMerge.MERGE)
    @XMerge(defaultAssignment = false) // compat
    protected boolean merge = false;

    @XNodeList(value = "factories/factory", type = ArrayList.class, componentType = ActiveFileSystemItemFactoryDescriptor.class)
    protected List<ActiveFileSystemItemFactoryDescriptor> factories;

    public List<ActiveFileSystemItemFactoryDescriptor> getFactories() {
        return Collections.unmodifiableList(factories);
    }

    /** @since 11.5 */
    public Set<String> getActiveFactories() {
        Set<String> res = new HashSet<>();
        factories.forEach(desc -> {
            String name = desc.getName();
            if (desc.isEnabled()) {
                res.add(name);
            } else {
                res.remove(name);
            }
        });
        return res;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<merge = ");
        sb.append(merge);
        sb.append(", [");
        for (ActiveFileSystemItemFactoryDescriptor factory : factories) {
            sb.append(factory);
            sb.append(", ");
        }
        sb.append("]>");
        return sb.toString();
    }

}
