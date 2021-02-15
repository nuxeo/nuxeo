/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.directory.memory;

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.ecm.directory.BaseDirectoryDescriptor;
import org.nuxeo.ecm.directory.Directory;

/**
 * @since 5.6
 */
@XObject("directory")
@XRegistry
public class MemoryDirectoryDescriptor extends BaseDirectoryDescriptor {

    @XNodeList(value = "schemaSet/field", type = HashSet.class, componentType = String.class)
    public Set<String> schemaSet;

    @Override
    public Directory newDirectory() {
        return new MemoryDirectory(this);
    }

    @Override
    public void merge(BaseDirectoryDescriptor other) {
        super.merge(other);
        if (other instanceof MemoryDirectoryDescriptor) {
            merge((MemoryDirectoryDescriptor) other);
        }
    }

    protected void merge(MemoryDirectoryDescriptor other) {
        if (other.schemaSet != null && !other.schemaSet.isEmpty()) {
            schemaSet = other.schemaSet;
        }
    }

    @Override
    public MemoryDirectoryDescriptor clone() {
        MemoryDirectoryDescriptor clone = (MemoryDirectoryDescriptor) super.clone();
        if (schemaSet != null) {
            clone.schemaSet = new HashSet<>(schemaSet);
        }
        return clone;
    }

}
