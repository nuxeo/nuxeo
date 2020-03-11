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

/**
 * Special no operation codec, can be used as marker.
 *
 * @since 10.2
 */
public class NoCodec implements Codec {

    public static final String NAME = "none";

    public static final NoCodec NO_CODEC = new NoCodec();

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public byte[] encode(Object object) {
        throw new IllegalStateException("NoCodec should not be used");
    }

    @Override
    public Object decode(byte[] data) {
        throw new IllegalStateException("NoCodec should not be used");
    }
}
