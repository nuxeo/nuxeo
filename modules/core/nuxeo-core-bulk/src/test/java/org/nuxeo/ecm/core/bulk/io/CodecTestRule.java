/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.bulk.io;

import static java.util.Objects.requireNonNull;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;

/**
 * A JUnit test {@link TestRule} to ease tests around {@link Codec}.
 *
 * @param <T> The class name of the object
 * @since 10.3
 */
public class CodecTestRule<T> implements TestRule {

    protected final String codecName;

    protected final Class<T> objectClass;

    protected Codec<T> codec;

    public CodecTestRule(String codecName, Class<T> objectClass) {
        this.codecName = requireNonNull(codecName);
        this.objectClass = requireNonNull(objectClass);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                codec = Framework.getService(CodecService.class).getCodec(codecName, objectClass);
                try {
                    base.evaluate();
                } finally {
                    codec = null;
                }
            }
        };
    }

    /**
     * @return the result after encoding then decoding given object.
     */
    public T encodeDecode(T object) {
        byte[] bytes = codec.encode(object);
        return codec.decode(bytes);
    }

}
