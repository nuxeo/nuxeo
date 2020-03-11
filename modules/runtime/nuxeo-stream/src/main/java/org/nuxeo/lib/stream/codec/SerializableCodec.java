/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.lib.stream.codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Java {@link java.io.Serializable} encoding. It is highly recommended to use {@link java.io.Externalizable}, for
 * performance reason.
 *
 * @since 10.2
 */
public class SerializableCodec<T extends Serializable> implements Codec<T> {

    public static final String NAME = "java";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public byte[] encode(T object) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
            out.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @SuppressWarnings({ "unchecked", "squid:S2093" })
    @Override
    public T decode(byte[] data) {
        // TODO: check if it worth to switch to commons-lang3 SerializationUtils
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInput in = null;
        try {
            in = new ObjectInputStream(bis);
            return (T) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                bis.close();
            } catch (IOException ex) {
                // we want to ignore close exception so no try-with-resources squid:S2093
            }
        }
    }
}
