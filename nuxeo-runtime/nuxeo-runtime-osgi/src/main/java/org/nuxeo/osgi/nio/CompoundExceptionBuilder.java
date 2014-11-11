package org.nuxeo.osgi.nio;

import java.util.LinkedList;
import java.util.List;

public abstract class CompoundExceptionBuilder<T extends Throwable> {

    protected final List<T> accumulated = new LinkedList<T>();

    protected abstract T newThrowable(List<T> causes);

    public void add(T error) {
        accumulated.add(error);
    }

    public void throwOnError() throws T {
        if (accumulated.isEmpty()) {
            return;
        }
        throw newThrowable(accumulated);
    }
}