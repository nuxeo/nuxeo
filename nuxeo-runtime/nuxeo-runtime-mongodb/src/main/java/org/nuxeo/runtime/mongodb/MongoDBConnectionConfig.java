/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.runtime.mongodb;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor to retrieve connection information to MongoDB.
 *
 * @since 9.1
 */
@XObject("connection")
public class MongoDBConnectionConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@id")
    private String id;

    @XNode("server")
    private String server;

    @XNode("dbname")
    private String dbname;

    /**
     * For deserialization
     */
    protected MongoDBConnectionConfig() {
        //nothing
    }

    public MongoDBConnectionConfig(String id, String server, String dbname) {
        this.id = id;
        this.server = server;
        this.dbname = dbname;
    }

    public String getId() {
        return id;
    }

    public String getServer() {
        return server;
    }

    public String getDbname() {
        return dbname;
    }

}
