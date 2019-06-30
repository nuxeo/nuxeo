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

import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.Record;

/**
 * Factory to generate Record compliant with Confluent Avro, ready to be used by ksqlDB.
 *
 * @since 11.4
 */
public class AvroRecordCodecFactory implements CodecFactory {

    public static final String KEY_SCHEMA_REGISTRY_URLS = "schemaRegistryUrls";

    public static final String DEFAULT_SCHEMA_REGISTRY_URLS = "http://localhost:8081";

    public static final String KEY_MESSAGE_CLASS = "messageClass";

    protected String messageClassName;

    protected String schemaRegistryUrls;

    @Override
    public void init(Map<String, String> options) {
        messageClassName = options.get(KEY_MESSAGE_CLASS);
        if (messageClassName == null) {
            throw new IllegalArgumentException("AvroRecordCodecFactory requires a messageClass option.");
        }
        schemaRegistryUrls = options.getOrDefault(KEY_SCHEMA_REGISTRY_URLS, DEFAULT_SCHEMA_REGISTRY_URLS);
    }

    @Override
    public <T> Codec<T> newCodec(Class<T> objectClass) {
        if (!objectClass.isAssignableFrom(Record.class)) {
            throw new IllegalArgumentException(
                    "AvroRecordCodecFactory works only Computation Record not: " + objectClass);
        }
        try {
            return new AvroRecordCodec(messageClassName, schemaRegistryUrls);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Invalid messageClass: " + messageClassName);
        }
    }
}
