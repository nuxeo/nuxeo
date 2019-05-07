/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.common.function;

/**
 * @param <E> the type of exception to throw
 * @since 11.1
 */
@FunctionalInterface
public interface ThrowableRunnable<E extends Throwable> {

    void run() throws E;

    /**
     * @return this {@link ThrowableRunnable} as a {@link Runnable} throwing the checked exception as an unchecked one
     */
    default Runnable toRunnable() {
        return asRunnable(this);
    }

    /**
     * @return this {@link ThrowableRunnable} as a {@link ThrowableSupplier} returning {@link Void}
     */
    default ThrowableSupplier<Void, E> toThrowableSupplier() {
        return asThrowableSupplier(this);
    }

    /**
     * @return the given {@link ThrowableRunnable} as a {@link Runnable} throwing the checked exception as an unchecked
     *         one
     */
    static <E extends Throwable> Runnable asRunnable(ThrowableRunnable<E> throwableRunnable) {
        return () -> {
            try {
                throwableRunnable.run();
            } catch (Throwable t) { // NOSONAR
                FunctionUtils.sneakyThrow(t);
            }
        };
    }

    /**
     * @return the given {@link ThrowableRunnable} as a {@link ThrowableSupplier} returning {@link Void}
     */
    static <E extends Throwable> ThrowableSupplier<Void, E> asThrowableSupplier(
            ThrowableRunnable<E> throwableRunnable) {
        return () -> {
            throwableRunnable.run();
            return null;
        };
    }

}
