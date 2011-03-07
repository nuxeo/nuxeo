package org.nuxeo.ecm.platform.wi.backend;

/**
 * @author Organization: Gagnavarslan ehf
 */
public class SimpleBackendFactory extends AbstractBackendFactory {

    @Override
    protected Backend createRootBackend() {
        return new SimpleRootBackend();
    }
}
