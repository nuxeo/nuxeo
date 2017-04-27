/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.directory.mongodb;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.directory.BaseDirectoryDescriptor;
import org.nuxeo.ecm.directory.InverseReference;
import org.nuxeo.ecm.directory.Reference;

/**
 * @since 9.1
 */
@XObject("directory")
public class MongoDBDirectoryDescriptor extends BaseDirectoryDescriptor {

    @XNode("serverUrl")
    public String serverUrl;

    @XNode("databaseName")
    public String databaseName;

    @XNodeList(value = "references/reference", type = MongoDBReference[].class, componentType = MongoDBReference.class)
    public MongoDBReference[] references;

    @XNodeList(value = "references/inverseReference", type = InverseReference[].class, componentType = InverseReference.class)
    public InverseReference[] inverseReferences;

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public Reference[] getInverseReferences() {
        return inverseReferences;
    }

    public Reference[] getMongoDBReferences() {
        return references;
    }

    @Override
    public void merge(BaseDirectoryDescriptor other) {
        super.merge(other);
        if (other instanceof MongoDBDirectoryDescriptor) {
            merge((MongoDBDirectoryDescriptor) other);
        }
    }

    protected void merge(MongoDBDirectoryDescriptor other) {
        if (other.serverUrl != null) {
            serverUrl = other.serverUrl;
        }
        if (other.databaseName != null) {
            databaseName = other.databaseName;
        }
        if (other.inverseReferences != null && other.inverseReferences.length != 0) {
            inverseReferences = other.inverseReferences;
        }
        if (other.references != null && other.references.length != 0) {
            references = other.references;
        }
    }

    @Override
    public MongoDBDirectoryDescriptor clone() {
        MongoDBDirectoryDescriptor clone = (MongoDBDirectoryDescriptor) super.clone();
        if (references != null) {
            clone.references = new MongoDBReference[references.length];
            for (int i = 0; i < references.length; i++) {
                clone.references[i] = references[i].clone();
            }
        }
        if (inverseReferences != null) {
            clone.inverseReferences = new InverseReference[inverseReferences.length];
            for (int i = 0; i < inverseReferences.length; i++) {
                clone.inverseReferences[i] = inverseReferences[i].clone();
            }
        }
        return clone;
    }

    @Override
    public MongoDBDirectory newDirectory() {
        return new MongoDBDirectory(this);
    }

}
