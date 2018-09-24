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
import org.nuxeo.lib.stream.codec.AvroConfluentCodec;
import org.nuxeo.lib.stream.codec.AvroJsonCodec;
import org.nuxeo.lib.stream.codec.AvroMessageCodec;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.avro.AvroService;

/**
 * Factory to generate Avro codec with different flavors
 *
 * @since 10.3
 */
public class AvroCodecFactory implements CodecFactory {

    public static final String KEY_SCHEMA_REGISTRY_URLS = "schemaRegistryUrls";

    public static final String DEFAULT_SCHEMA_REGISTRY_URLS = "http://localhost:8081";

    public static final String KEY_ENCODING = "encoding";

    public static final String DEFAULT_ENCODING = "default";

    protected String encoding;

    protected String schemaRegistryUrls;

    @Override
    public void init(Map<String, String> options) {
        this.encoding = options.getOrDefault(KEY_ENCODING, DEFAULT_ENCODING);
        this.schemaRegistryUrls = options.getOrDefault(KEY_SCHEMA_REGISTRY_URLS, DEFAULT_SCHEMA_REGISTRY_URLS);
    }

    @Override
    public <T> Codec<T> newCodec(Class<T> objectClass) {
        switch (encoding) {
        case "json":
            return new AvroJsonCodec<>(objectClass);
        case "binary":
            return new AvroBinaryCodec<>(objectClass);
        case "confluent":
            return new AvroConfluentCodec<>(objectClass, schemaRegistryUrls);
        case "message":
        default:
            return new AvroMessageCodec<>(objectClass, Framework.getService(AvroService.class).getSchemaStore());
        }
    }
}
