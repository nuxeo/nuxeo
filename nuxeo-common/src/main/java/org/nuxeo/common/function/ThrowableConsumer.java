/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.common.function;

import java.util.function.Consumer;

/**
 * @param <T> the type of the input to the operation
 * @param <E> the type of exception to throw
 * @since 11.1
 */
@FunctionalInterface
public interface ThrowableConsumer<T, E extends Throwable> {

    void accept(T t) throws E;

    /**
     * @return this {@link ThrowableConsumer} as a {@link Consumer} throwing the checked exception as an unchecked one
     */
    default Consumer<T> toConsumer() {
        return asConsumer(this);
    }

    /**
     * @return the given {@link ThrowableConsumer} as a {@link Consumer} throwing the checked exception as an unchecked
     *         one
     */
    static <T, E extends Throwable> Consumer<T> asConsumer(ThrowableConsumer<T, E> throwableConsumer) {
        return arg -> {
            try {
                throwableConsumer.accept(arg);
            } catch (Throwable t) { // NOSONAR
                FunctionUtils.sneakyThrow(t);
            }
        };
    }

}
