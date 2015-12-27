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

/**
 * MongoDB Repository Descriptor.
 */
@XObject(value = "repository")
public class MongoDBRepositoryDescriptor {

    public MongoDBRepositoryDescriptor() {
    }

    /** False if the boolean is null or FALSE, true otherwise. */
    private static boolean defaultFalse(Boolean bool) {
        return Boolean.TRUE.equals(bool);
    }

    @XNode("@name")
    public String name;

    @XNode("@label")
    public String label;

    @XNode("@isDefault")
    private Boolean isDefault;

    public Boolean isDefault() {
        return isDefault;
    }

    @XNode("fulltext@disabled")
    private Boolean fulltextDisabled;

    public boolean getFulltextDisabled() {
        return defaultFalse(fulltextDisabled);
    }

    // ----- MongoDB specific options -----

    @XNode("server")
    public String server;

    @XNode("dbname")
    public String dbname;

    /** Copy constructor. */
    public MongoDBRepositoryDescriptor(MongoDBRepositoryDescriptor other) {
        name = other.name;
        label = other.label;
        isDefault = other.isDefault;
        server = other.server;
        dbname = other.dbname;
        fulltextDisabled = other.fulltextDisabled;
    }

    public void merge(MongoDBRepositoryDescriptor other) {
        if (other.name != null) {
            name = other.name;
        }
        if (other.label != null) {
            label = other.label;
        }
        if (other.isDefault != null) {
            isDefault = other.isDefault;
        }
        if (other.server != null) {
            server = other.server;
        }
        if (other.dbname != null) {
            dbname = other.dbname;
        }
        if (other.fulltextDisabled != null) {
            fulltextDisabled = other.fulltextDisabled;
        }
    }

}
