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
