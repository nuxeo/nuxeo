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

import java.util.function.BiConsumer;

/**
 * @param <T> the type of the first argument to the operation
 * @param <U> the type of the second argument to the operation
 * @param <E> the type of exception to throw
 * @since 11.5
 */
@FunctionalInterface
public interface ThrowableBiConsumer<T, U, E extends Throwable> {

    void accept(T t, U u) throws E;

    /**
     * @return this {@link ThrowableBiConsumer} as a {@link BiConsumer} throwing the checked exception as an unchecked
     *         one
     */
    default BiConsumer<T, U> toBiConsumer() {
        return asBiConsumer(this);
    }

    /**
     * @return the given {@link ThrowableBiConsumer} as a {@link BiConsumer} throwing the checked exception as an
     *         unchecked one
     */
    static <T, U, E extends Throwable> BiConsumer<T, U> asBiConsumer(ThrowableBiConsumer<T, U, E> throwableBiConsumer) {
        return (l, r) -> {
            try {
                throwableBiConsumer.accept(l, r);
            } catch (Throwable t) { // NOSONAR
                FunctionUtils.sneakyThrow(t);
            }
        };
    }

}
