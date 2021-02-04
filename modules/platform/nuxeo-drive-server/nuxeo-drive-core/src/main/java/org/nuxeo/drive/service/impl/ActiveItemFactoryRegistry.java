/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.drive.service.impl;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.common.xmap.XAnnotatedObject;
import org.nuxeo.common.xmap.registry.Registry;
import org.nuxeo.common.xmap.registry.SingleRegistry;
import org.w3c.dom.Element;

/**
 * Registry handling two types of descriptors.
 *
 * @see ActiveFileSystemItemFactoriesDescriptor
 * @see ActiveTopLevelFolderItemFactoryDescriptor
 * @since 11.5
 */
public class ActiveItemFactoryRegistry implements Registry {

    public static final String TOP_LEVEL_NODE_NAME = "activeTopLevelFolderItemFactory";

    public static final String FILE_SYSTEM_NODE_NAME = "activeFileSystemItemFactories";

    protected final SingleRegistry topLevelFolderRegistry = new SingleRegistry();

    protected final SingleRegistry fileSystemRegistry = new SingleRegistry();

    @Override
    public void initialize() {
        topLevelFolderRegistry.initialize();
        fileSystemRegistry.initialize();
    }

    @Override
    public void tag(String id) {
        topLevelFolderRegistry.tag(id);
        fileSystemRegistry.tag(id);
    }

    @Override
    public boolean isTagged(String id) {
        return topLevelFolderRegistry.isTagged(id) || fileSystemRegistry.isTagged(id);
    }

    @Override
    public void register(Context ctx, XAnnotatedObject xObject, Element element, String tag) {
        String nodeName = element.getNodeName();
        if (TOP_LEVEL_NODE_NAME.equals(nodeName)) {
            topLevelFolderRegistry.register(ctx, xObject, element, tag);
        } else if (FILE_SYSTEM_NODE_NAME.equals(nodeName)) {
            fileSystemRegistry.register(ctx, xObject, element, tag);
        }
    }

    @Override
    public void unregister(String tag) {
        topLevelFolderRegistry.unregister(tag);
        fileSystemRegistry.unregister(tag);
    }

    public Optional<String> getTopLevelFolderItemFactory() {
        return topLevelFolderRegistry.<ActiveTopLevelFolderItemFactoryDescriptor> getContribution()
                                     .map(ActiveTopLevelFolderItemFactoryDescriptor::getName);
    }

    public Set<String> getFileSystemItemFactories() {
        Optional<ActiveFileSystemItemFactoriesDescriptor> res = fileSystemRegistry.getContribution();
        if (res.isPresent()) {
            return res.get().getActiveFactories();
        }
        return Collections.emptySet();
    }

}
