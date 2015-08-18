package org.nuxeo.ecm.core.redis.contribs;/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Delbosc Benoit
 */

import org.apache.commons.codec.binary.Base64;
import org.nuxeo.ecm.core.storage.sql.Invalidations;

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
    private Invalidations invalidations;

    public RedisInvalidations(String nodeId, Invalidations invals) {
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

    private Invalidations deserializeInvalidations(String invalsStr) {
        InputStream bain = new ByteArrayInputStream(Base64.decodeBase64(invalsStr));
        try (ObjectInputStream in = new ObjectInputStream(bain)) {
            return (Invalidations) in.readObject();
        } catch (IOException | ClassNotFoundException cause) {
            throw new IllegalArgumentException("Cannot deserialize invalidations", cause);
        }
    }

    public Invalidations getInvalidations() {
        return invalidations;
    }

    public String serialize() throws IOException {
        return nodeId + ":" + serializeInvalidations(invalidations);
    }

    private String serializeInvalidations(Invalidations invals) throws IOException {
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
