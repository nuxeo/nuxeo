package org.nuxeo.osgi.nio;

import java.io.IOException;
import java.util.List;

public class CompoundIOExceptionBuilder extends
        CompoundExceptionBuilder<IOException> {

    @Override
    protected IOException newThrowable(List<IOException> causes) {
        return new CompoundIOException(
                causes.toArray(new IOException[causes.size()]));
    }

}