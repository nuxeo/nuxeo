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

/**
 * Factory to init and provide codec object.
 *
 * @since 10.2
 */
public interface CodecFactory {

    /**
     * Initializes the codec factory using a map of options.
     */
    void init(Map<String, String> options);

    /**
     * Returns a codec object enables to encode/decode object ot class T.
     */
    <T> Codec<T> newCodec(Class<T> objectClass);
}
