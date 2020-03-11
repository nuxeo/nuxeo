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
 *     Delbosc Benoit
 */
package org.nuxeo.ecm.core.redis.contribs;

import org.nuxeo.ecm.core.storage.dbs.DBSInvalidations;

/**
 * invalidations and nodeId serializer/unserializer.
 *
 * @since 8.10
 */
public class RedisDBSInvalidations {

    private static final String MESSAGE_SEPARATOR = ":";

    private static final String ID_SEPARATOR = "/";

    private static final String ALL_DOCUMENTS = "ALL";

    private String nodeId;

    private DBSInvalidations invalidations;

    public RedisDBSInvalidations(String nodeId, DBSInvalidations invals) {
        assert nodeId != null : "nodeId required";
        assert invals != null : "invals required";
        this.nodeId = nodeId;
        this.invalidations = invals;
    }

    public RedisDBSInvalidations(String receiverNodeId, String message) {
        assert receiverNodeId != null : "receiverNodeId required";
        if (message == null || !message.contains(":")) {
            throw new IllegalArgumentException("Invalid message: " + message);
        }
        String[] parts = message.split(MESSAGE_SEPARATOR, 2);
        nodeId = parts[0];
        if (!receiverNodeId.equals(nodeId)) {
            // only decode if it is a remote node
            invalidations = deserializeInvalidations(parts[1]);
        }
    }

    private DBSInvalidations deserializeInvalidations(String invalsStr) {
        if (ALL_DOCUMENTS.equals(invalsStr)) {
            return new DBSInvalidations(true);
        }
        DBSInvalidations invals = new DBSInvalidations();
        for (String id : invalsStr.split(ID_SEPARATOR)) {
            invals.add(id);
        }
        return invals;
    }

    public DBSInvalidations getInvalidations() {
        return invalidations;
    }

    public String serialize() {
        // message:
        // - nodeId:id1/id2/...
        // - nodeId:ALL
        return nodeId + MESSAGE_SEPARATOR + serializeInvalidations(invalidations);
    }

    private String serializeInvalidations(DBSInvalidations invals) {
        if (invals.all) {
            return ALL_DOCUMENTS;
        }
        return String.join(ID_SEPARATOR, invals.ids);
    }

    @Override
    public String toString() {
        if (invalidations == null) {
            return "RedisDBSInvalidationsInvalidations(local, discarded)";
        }
        return "RedisDBSInvalidationsInvalidations(fromNode=" + nodeId + ", " + invalidations.toString() + ")";
    }

}
