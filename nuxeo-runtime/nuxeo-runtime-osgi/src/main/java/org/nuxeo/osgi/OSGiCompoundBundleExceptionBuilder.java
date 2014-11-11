package org.nuxeo.osgi;

import java.util.List;

import org.nuxeo.osgi.nio.CompoundExceptionBuilder;
import org.osgi.framework.BundleException;

public class OSGiCompoundBundleExceptionBuilder extends
        CompoundExceptionBuilder<BundleException> {

    @Override
    protected BundleException newThrowable(final List<BundleException> pff) {
        return new BundleException("compound exception, watch other causes", pff.get(0)) {

            public final BundleException[] causes = pff.toArray(new BundleException[pff.size()]);

            private static final long serialVersionUID = 1L;

            public BundleException[] getCauses() {
                return causes;
            }

            @Override
            public Throwable getCause() {
                return causes[0];
            }
        };
    }

}
