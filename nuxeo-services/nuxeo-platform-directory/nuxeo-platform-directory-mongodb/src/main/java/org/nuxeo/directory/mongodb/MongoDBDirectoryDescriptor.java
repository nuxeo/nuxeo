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
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.directory.BaseDirectoryDescriptor;

/**
 * @since 9.1
 */
@XObject("directory")
public class MongoDBDirectoryDescriptor extends BaseDirectoryDescriptor {

    @XNode("serverUrl")
    public String serverUrl;

    @XNode("databaseName")
    public String databaseName;

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
    }

    @Override
    public MongoDBDirectory newDirectory() {
        return new MongoDBDirectory(this);
    }

}
