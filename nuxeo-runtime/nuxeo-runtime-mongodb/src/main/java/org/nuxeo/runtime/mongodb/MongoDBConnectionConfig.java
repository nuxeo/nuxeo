/*
 * (C) Copyright 2017-2018 Nuxeo (http://nuxeo.com/) and others.
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

import static org.apache.commons.lang3.StringUtils.defaultString;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * Descriptor to retrieve connection information to MongoDB.
 *
 * @since 9.1
 */
@XObject("connection")
public class MongoDBConnectionConfig implements Descriptor {

    @XNode("@id")
    public String id;

    @XNode("server")
    public String server;

    /** @since 10.3 */
    @XNode("ssl")
    public Boolean ssl;

    /** @since 10.3 */
    @XNode("trustStorePath")
    public String trustStorePath;

    /** @since 10.3 */
    @XNode("trustStorePassword")
    public String trustStorePassword;

    /** @since 10.3 */
    @XNode("trustStoreType")
    public String trustStoreType;

    /** @since 10.3 */
    @XNode("keyStorePath")
    public String keyStorePath;

    /** @since 10.3 */
    @XNode("keyStorePassword")
    public String keyStorePassword;

    /** @since 10.3 */
    @XNode("keyStoreType")
    public String keyStoreType;

    @XNode("dbname")
    public String dbname;

    /** @since 11.1 */
    @XNode("maxTime")
    public Duration maxTime;

    @XNodeMap(value = "property", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> properties = new HashMap<>();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public MongoDBConnectionConfig merge(Descriptor o) {
        MongoDBConnectionConfig other = (MongoDBConnectionConfig) o;
        MongoDBConnectionConfig merged = new MongoDBConnectionConfig();
        merged.id = id;
        merged.server = defaultString(other.server, server);
        merged.ssl = other.ssl != null ? other.ssl : ssl;
        merged.trustStorePath = defaultString(other.trustStorePath, trustStorePath);
        merged.trustStorePassword = defaultString(other.trustStorePassword, trustStorePassword);
        merged.trustStoreType = defaultString(other.trustStoreType, trustStoreType);
        merged.keyStorePath = defaultString(other.keyStorePath, keyStorePath);
        merged.keyStorePassword = defaultString(other.keyStorePassword, keyStorePassword);
        merged.keyStoreType = defaultString(other.keyStoreType, keyStoreType);
        merged.dbname = defaultString(other.dbname, dbname);
        merged.maxTime = other.maxTime != null ? other.maxTime : maxTime;
        merged.properties.putAll(properties);
        merged.properties.putAll(other.properties);
        return merged;
    }

}
