package org.nuxeo.runtime.model;

import java.util.List;

public class RuntimeModelException extends Exception {

    private static final long serialVersionUID = 1L;

    public RuntimeModelException(String message, Throwable cause) {
        super(message, cause);
    }

    public RuntimeModelException(String message) {
        super(message);
    }

    public static CompoundBuilder newErrors() {
        return new CompoundBuilder();
    }

    public static class CompoundBuilder extends org.nuxeo.common.errors.CompoundExceptionBuilder<RuntimeModelException> {

        @Override
        protected RuntimeModelException newThrowable(
                List<RuntimeModelException> causes) {
            return new CompoundException(causes.toArray(new RuntimeModelException[causes.size()]));
        }

    }

    public static class CompoundException extends RuntimeModelException {
        private static final long serialVersionUID = 1L;
        public final RuntimeModelException causes[];
        public CompoundException(RuntimeModelException[] causes) {
            super("caught multiple exception, first occurence", causes[0]);
            this.causes = causes;
        }
    }
}
