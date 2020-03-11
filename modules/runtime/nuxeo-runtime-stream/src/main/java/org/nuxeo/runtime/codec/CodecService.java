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

import org.nuxeo.lib.stream.codec.Codec;

/**
 * Gives access to coder/decoder for a class .
 *
 * @since 10.2
 */
public interface CodecService {

    /**
     * Returns a codec able to code and decode object of type T
     *
     * @param codecName the name of the registered codec implementation
     * @param objectClass the object class of the object to encode decode
     * @param <T> The class name of the object
     */
    <T> Codec<T> getCodec(String codecName, Class<T> objectClass);

}
