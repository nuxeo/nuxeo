package org.nuxeo.ecm.platform.importer.source;/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */

import org.nuxeo.ecm.core.api.Blob;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 9.1
 */
public class ImmutableNode implements Node {
    private String type;
    private String parentPath;
    private String name;
    private int partition;
    private Map<String, Serializable> properties;
    private Blob blob;

    private ImmutableNode(ImmutableNodeBuilder builder) {
        type = builder.type;
        parentPath = builder.parentPath;
        name = builder.name;
        this.partition = builder.partition;
        properties = builder.properties;
        blob = builder.blob;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPath() {
        return parentPath + "/" + name;
    }

    public int getPartition() {
        return partition;
    }

    /**
     * Type of the document
     */
    public String getType() {
        return type;
    }

    public String getParentPath() {
        return parentPath;
    }

    public Map<String, Serializable> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public Blob getBlob() {
        // TODO should return unmodifialble blob
        return blob;
    }

    public static class ImmutableNodeBuilder {
        private String name;
        private String parentPath;
        private String type;
        private Map<String, Serializable> properties;
        private Blob blob;
        private int partition;

        /**
         * Helper to build an immutable node.
         *
         * @param type the type of document
         * @param parentPath the container path where the document should be created
         * @param name the name of the document
         */
        public ImmutableNodeBuilder(String type, String parentPath, String name) {
            this.type = type;
            this.parentPath = parentPath;
            this.name = name;
        }

        public ImmutableNodeBuilder setProperties(HashMap<String, Serializable> properties) {
            this.properties = (Map) properties.clone();
            return this;
        }

        public ImmutableNodeBuilder setBlob(Blob blob) {
            this.blob = blob;
            return this;
        }

        /**
         * Nodes that belong to the same partition are ordered by the producer, they will be consumed in the same order.
         */
        public ImmutableNodeBuilder setPartition(int partition) {
            this.partition = partition;
            return this;
        }

        public ImmutableNode build() {
            return new ImmutableNode(this);
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(type);
        out.writeObject(parentPath);
        out.writeObject(name);
        out.writeInt(partition);
        int nbProperties = (properties == null ? 0: properties.size());
        out.writeInt(nbProperties);
        if (properties != null) {
            for (Map.Entry<String, Serializable> entry : properties.entrySet()) {
                out.writeObject(entry.getKey());
                out.writeObject(entry.getValue());
            }
        }
        if (blob != null) {
            out.writeBoolean(true);
            out.writeObject(blob);
        } else {
            out.writeBoolean(false);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        type = (String) in.readObject();
        parentPath = (String) in.readObject();
        name = (String) in.readObject();
        partition = in.readInt();
        int nbProperties = in.readInt();
        if (nbProperties > 0) {
            properties = new HashMap<>(nbProperties);
            while (nbProperties > 0) {
                String key = (String) in.readObject();
                Serializable value = (Serializable) in.readObject();
                properties.put(key, value);
                nbProperties--;
            }
        }
        if (in.readBoolean()) {
            blob = (Blob) in.readObject();
        }
    }
}

