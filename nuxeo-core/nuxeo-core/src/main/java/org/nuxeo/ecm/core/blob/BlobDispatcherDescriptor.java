/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob;

import java.util.LinkedHashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Descriptor for a {@link BlobDispatcher} and its configuration.
 *
 * @since 7.3
 */
@XObject(value = "blobdispatcher")
public class BlobDispatcherDescriptor implements Descriptor {

    /** @since 2021.15 */
    @XNode("@merge")
    protected boolean merge;

    @XNode("class")
    public Class<? extends BlobDispatcher> klass = DefaultBlobDispatcher.class;

    @XNodeMap(value = "property", key = "@name", type = LinkedHashMap.class, componentType = String.class)
    public Map<String, String> properties = new LinkedHashMap<>();

    private BlobDispatcher instance;

    @Override
    public String getId() {
        return UNIQUE_DESCRIPTOR_ID;
    }

    public synchronized BlobDispatcher getBlobDispatcher() {
        if (instance == null) {
            BlobDispatcher blobDispatcher;
            try {
                blobDispatcher = klass.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new NuxeoException(e);
            }
            blobDispatcher.initialize(properties);
            instance = blobDispatcher;
        }
        return instance;
    }

    @Override
    public Descriptor merge(Descriptor o) {
        BlobDispatcherDescriptor other = (BlobDispatcherDescriptor) o;
        BlobDispatcherDescriptor merged = new BlobDispatcherDescriptor();
        if (other.merge) {
            if (klass != null && other.klass != null && !klass.equals(other.klass)) {
                throw new NuxeoException(
                        "Can not merge blob dispatcher with different class: " + klass + " and " + other.klass);
            }
            merged.klass = klass;
            merged.properties.putAll(properties);
            merged.properties.putAll(other.properties);
        } else {
            merged.klass = other.klass;
            merged.properties.putAll(other.properties);
        }
        return merged;
    }
}
