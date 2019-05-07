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

import java.util.function.Function;

/**
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 * @param <E> the type of exception to throw
 * @since 11.1
 */
@FunctionalInterface
public interface ThrowableFunction<T, R, E extends Throwable> {

    R apply(T t) throws E;

    /**
     * @return this {@link ThrowableFunction} as a {@link Function} throwing the checked exception as an unchecked one
     */
    default Function<T, R> toFunction() {
        return asFunction(this);
    }

    /**
     * @return the given {@link ThrowableFunction} as a {@link Function} throwing the checked exception as an unchecked
     *         one
     */
    static <T, R, E extends Throwable> Function<T, R> asFunction(ThrowableFunction<T, R, E> throwableFunction) {
        return arg -> {
            try {
                return throwableFunction.apply(arg);
            } catch (Throwable t) { // NOSONAR
                return FunctionUtils.sneakyThrow(t); // will never return
            }
        };
    }

}
