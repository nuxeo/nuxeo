/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.mongodb;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.storage.dbs.DBSRepositoryDescriptor;

/**
 * MongoDB Repository Descriptor.
 */
@XObject(value = "repository")
public class MongoDBRepositoryDescriptor extends DBSRepositoryDescriptor {

    public MongoDBRepositoryDescriptor() {
    }

    /**
     * @deprecated since 9.3 you should now use MongoDBConnectionService to define connections
     * @see org.nuxeo.runtime.mongodb.MongoDBConnectionService
     * @see org.nuxeo.runtime.mongodb.MongoDBComponent
     */
    @Deprecated
    @XNode("server")
    public String server;

    /**
     * @deprecated since 9.3 you should now use MongoDBConnectionService to define connections
     * @see org.nuxeo.runtime.mongodb.MongoDBConnectionService
     * @see org.nuxeo.runtime.mongodb.MongoDBComponent
     */
    @Deprecated
    @XNode("dbname")
    public String dbname;

    @XNode("nativeId")
    public Boolean nativeId;

    @XNode("sequenceBlockSize")
    public Integer sequenceBlockSize;

    /** @since 11.1 **/
    @XNode("childNameUniqueConstraintEnabled")
    public Boolean childNameUniqueConstraintEnabled;

    /** @since 11.1 **/
    public Boolean getChildNameUniqueConstraintEnabled() {
        return childNameUniqueConstraintEnabled;
    }

    @Override
    public MongoDBRepositoryDescriptor clone() {
        return (MongoDBRepositoryDescriptor) super.clone();
    }

    @Override
    public void merge(DBSRepositoryDescriptor o) {
        super.merge(o);
        if (!(o instanceof MongoDBRepositoryDescriptor)) {
            return;
        }
        MongoDBRepositoryDescriptor other = (MongoDBRepositoryDescriptor) o;
        if (other.server != null) {
            server = other.server;
        }
        if (other.dbname != null) {
            dbname = other.dbname;
        }
        if (other.nativeId != null) {
            nativeId = other.nativeId;
        }
        if (other.sequenceBlockSize != null) {
            sequenceBlockSize = other.sequenceBlockSize;
        }
        if (other.childNameUniqueConstraintEnabled != null) {
            childNameUniqueConstraintEnabled = other.childNameUniqueConstraintEnabled;
        }
    }

}
