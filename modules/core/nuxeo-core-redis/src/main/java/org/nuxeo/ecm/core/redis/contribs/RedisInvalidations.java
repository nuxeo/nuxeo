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

import org.apache.commons.codec.binary.Base64;
import org.nuxeo.ecm.core.storage.sql.VCSInvalidations;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * invalidations and nodeId serializer/unserializer.
 *
 * @since 7.4
 */
public class RedisInvalidations {
    private String nodeId;
    private VCSInvalidations invalidations;

    public RedisInvalidations(String nodeId, VCSInvalidations invals) {
        assert nodeId != null : "nodeId required";
        assert invals != null : "invals required";
        this.nodeId = nodeId;
        this.invalidations = invals;
    }

    public RedisInvalidations(String receiverNodeId, String message) {
        assert receiverNodeId != null : "receiverNodeId required";
        if (message == null || !message.contains(":")) {
            throw new IllegalArgumentException("Invalid message: " + message);
        }
        String[] parts = message.split(":", 2);
        nodeId = parts[0];
        if (!receiverNodeId.equals(nodeId)) {
            // only decode if it is a remote node
            invalidations = deserializeInvalidations(parts[1]);
        }
    }

    private VCSInvalidations deserializeInvalidations(String invalsStr) {
        InputStream bain = new ByteArrayInputStream(Base64.decodeBase64(invalsStr));
        try (ObjectInputStream in = new ObjectInputStream(bain)) {
            return (VCSInvalidations) in.readObject();
        } catch (IOException | ClassNotFoundException cause) {
            throw new IllegalArgumentException("Cannot deserialize invalidations", cause);
        }
    }

    public VCSInvalidations getInvalidations() {
        return invalidations;
    }

    public String serialize() throws IOException {
        return nodeId + ":" + serializeInvalidations(invalidations);
    }

    private String serializeInvalidations(VCSInvalidations invals) throws IOException {
        ByteArrayOutputStream baout = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baout);
        out.writeObject(invals);
        out.flush();
        out.close();
        // use base64 because Jedis don't have onMessage with bytes
        return Base64.encodeBase64String(baout.toByteArray());
    }

    @Override
    public String toString() {
        if (invalidations == null) {
            return "RedisInvalidationsInvalidations(local, discarded)";
        }
        return "RedisInvalidationsInvalidations(fromNode=" + nodeId + ", " + invalidations.toString() + ")";
    }

}
