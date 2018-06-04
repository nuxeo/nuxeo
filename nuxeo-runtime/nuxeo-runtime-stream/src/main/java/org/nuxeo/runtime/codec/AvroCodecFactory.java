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
package org.nuxeo.runtime.codec;

import java.util.Map;

import org.nuxeo.lib.stream.codec.AvroBinaryCodec;
import org.nuxeo.lib.stream.codec.AvroJsonCodec;
import org.nuxeo.lib.stream.codec.AvroMessageCodec;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.avro.AvroService;

/**
 * Factory to generate Avro codec with different flavors
 *
 * @since 10.2
 */
public class AvroCodecFactory implements CodecFactory {

    protected String encoding;

    @Override
    public void init(Map<String, String> options) {
        this.encoding = options.getOrDefault("encoding", "default");
    }

    @Override
    public <T> Codec<T> newCodec(Class<T> objectClass) {
        switch (encoding) {
        case "json":
            return new AvroJsonCodec<>(objectClass);
        case "binary":
            return new AvroBinaryCodec<>(objectClass);
        case "message":
        default:
            return new AvroMessageCodec<>(objectClass, Framework.getService(AvroService.class));
        }
    }
}
