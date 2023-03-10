/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.pubsub;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

/**
 * An action that needs to be propagated to all nodes in the cluster.
 *
 * @since 2023.0
 */
public class ClusterActionMessage implements SerializableMessage {

    private static final long serialVersionUID = 20220719L;

    protected static final String SEP = "@";

    public final String action;

    public final String param;

    public ClusterActionMessage(String action, String param) {
        this.action = action;
        this.param = param;
    }

    public static ClusterActionMessage deserialize(InputStream in) throws IOException {
        String string = IOUtils.toString(in, UTF_8);
        String[] parts = string.split(SEP, 2);
        if (parts.length != 2) {
            throw new IOException("Invalid cluster action: " + string);
        }
        return new ClusterActionMessage(parts[0], parts[1]);
    }

    @Override
    public void serialize(OutputStream out) throws IOException {
        String string = action + SEP + param;
        IOUtils.write(string, out, UTF_8);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + action + "," + param + ")";
    }
}
