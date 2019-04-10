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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.drive.service.FileSystemItemAdapterService;

/**
 * XMap descriptor for the {@code activeFileSystemItemFactories} contributions to the
 * {@code activeFileSystemItemFactories} extension point of the {@link FileSystemItemAdapterService}.
 *
 * @author Antoine Taillefer
 */
@XObject("activeFileSystemItemFactories")
public class ActiveFileSystemItemFactoriesDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@merge")
    protected boolean merge = false;

    @XNodeList(value = "factories/factory", type = ArrayList.class, componentType = ActiveFileSystemItemFactoryDescriptor.class)
    protected List<ActiveFileSystemItemFactoryDescriptor> factories; // NOSONAR, serialization is actually performed by
                                                                     // SerializationUtils#clone during merge/clone

    public boolean isMerge() {
        return merge;
    }

    public void setMerge(boolean merge) {
        this.merge = merge;
    }

    public List<ActiveFileSystemItemFactoryDescriptor> getFactories() {
        return factories;
    }

    public void setFactories(List<ActiveFileSystemItemFactoryDescriptor> factories) {
        this.factories = factories;
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
