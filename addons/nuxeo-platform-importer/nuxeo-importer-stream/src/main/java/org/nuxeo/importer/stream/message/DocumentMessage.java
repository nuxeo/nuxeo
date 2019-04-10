/*
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
package org.nuxeo.importer.stream.message;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.lib.stream.pattern.Message;

/**
 * Message that represent an immutable Nuxeo document.
 *
 * @since 9.1
 */
public class DocumentMessage implements Message {
    static final long serialVersionUID = 20170529L;

    protected String type;

    protected String parentPath;

    protected String name;

    protected Map<String, Serializable> properties;

    protected Blob blob;

    protected BlobInfo blobInfo;

    public DocumentMessage() {
    }

    protected DocumentMessage(Builder builder) {
        type = builder.type;
        parentPath = builder.parentPath;
        name = builder.name;
        properties = builder.properties;
        blob = builder.blob;
        blobInfo = builder.blobInfo;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getId() {
        return parentPath + "/" + name;
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
        // should return unmodifiable blob
        return blob;
    }

    public BlobInfo getBlobInfo() {
        return blobInfo;
    }

    /**
     * Helper to build a document message.
     *
     * @param type the type of document
     * @param parentPath the container path where the document should be created
     * @param name the name of the document
     */
    public static Builder builder(String type, String parentPath, String name) {
        return new Builder(type, parentPath, name);
    }

    public static DocumentMessage copy(DocumentMessage node, String newName) {
        Builder builder = builder(node.type, node.parentPath, newName);
        builder.blob = node.blob;
        builder.blobInfo = node.blobInfo;
        builder.properties = node.properties;
        return builder.build();
    }

    public static class Builder {
        protected String name;

        protected String parentPath;

        protected String type;

        protected Map<String, Serializable> properties;

        protected Blob blob;

        protected BlobInfo blobInfo;

        protected Builder(String type, String parentPath, String name) {
            this.type = type;
            this.parentPath = parentPath;
            this.name = name;
        }

        @SuppressWarnings("unchecked")
        public Builder setProperties(HashMap<String, Serializable> properties) {
            this.properties = (Map) properties.clone();
            return this;
        }

        public Builder setBlob(Blob blob) {
            this.blob = blob;
            return this;
        }

        public Builder setBlobInfo(BlobInfo blobInfo) {
            this.blobInfo = new BlobInfo(blobInfo);
            return this;
        }

        public String getName() {
            return name;
        }

        public String getParentPath() {
            return parentPath;
        }

        public String getType() {
            return type;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setParentPath(String parentPath) {
            this.parentPath = parentPath;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, Serializable> getProperties() {
            return properties;
        }

        public DocumentMessage build() {
            return new DocumentMessage(this);
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(type);
        out.writeObject(parentPath);
        out.writeObject(name);
        int nbProperties = (properties == null ? 0 : properties.size());
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
        if (blobInfo != null) {
            out.writeBoolean(true);
            out.writeObject(blobInfo.key);
            out.writeObject(blobInfo.digest);
            out.writeLong(blobInfo.length);
            out.writeObject(blobInfo.filename);
            out.writeObject(blobInfo.encoding);
            out.writeObject(blobInfo.mimeType);
        } else {
            out.writeBoolean(false);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        type = (String) in.readObject();
        parentPath = (String) in.readObject();
        name = (String) in.readObject();
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
        if (in.readBoolean()) {
            blobInfo = new BlobInfo();
            blobInfo.key = (String) in.readObject();
            blobInfo.digest = (String) in.readObject();
            blobInfo.length = in.readLong();
            blobInfo.filename = (String) in.readObject();
            blobInfo.encoding = (String) in.readObject();
            blobInfo.mimeType = (String) in.readObject();
        }

    }

    @Override
    public String toString() {
        String bi = "";
        if (blobInfo != null) {
            bi = String.format("blobInfo(key=%s filename=%s)", blobInfo.key, blobInfo.filename);
        } else if (blob != null) {
            bi = String.format("blob(filename=%s)", blob.getFilename());
        }
        return String.format("DocumentMessage(type=%s name=%s parentPath=%s bi=%s)", type, name, parentPath, bi);
    }
    // TODO: impl hashCode, equals, toString
}
