package org.nuxeo.ecm.platform.wi.backend.wss;

import org.nuxeo.ecm.platform.wi.backend.Backend;
import org.nuxeo.ecm.platform.wi.backend.BackendFactory;
import org.nuxeo.ecm.platform.wi.service.PluggableBackendFactory;
import org.nuxeo.wss.servlet.WSSRequest;
import org.nuxeo.wss.spi.WSSBackend;
import org.nuxeo.wss.spi.WSSBackendFactory;

/**
 * @author Organization: Gagnavarslan ehf
 */
public class WSSBackendFactoryImpl implements WSSBackendFactory {

    private BackendFactory factory = new PluggableBackendFactory();

    //for tests
    public void setFactory(BackendFactory factory) {
        this.factory = factory;
    }

    protected String computeVirtualRoot(WSSRequest request) {
        String virtualRoot = request.getSitePath();

        if (virtualRoot == null || virtualRoot.equals("")) {
            virtualRoot = request.getHttpRequest().getContextPath();
        }
        if (virtualRoot.startsWith("/")) {
            virtualRoot = virtualRoot.substring(1);
        }
        return virtualRoot;
    }

    @Override
    public WSSBackend getBackend(WSSRequest wssRequest) {
        String virtualRoot;
        if (wssRequest != null) {
            virtualRoot = computeVirtualRoot(wssRequest);
        } else {
            virtualRoot = "nuxeo";
        }
        Backend backend = null;
        if (wssRequest != null) {
            backend = factory.getBackend("/", wssRequest.getHttpRequest());
        } else {
            backend = factory.getBackend("/", null);
        }
        if (backend == null) {
            return new WSSFakeBackend();
        }
        if(backend.isRoot()){
            return new WSSRootBackendAdapter(backend, virtualRoot);
        } else if (backend.isVirtual()) {
            return new WSSVirtualBackendAdapter(backend, virtualRoot);
        } else {
            return new WSSBackendAdapter(backend, virtualRoot);
        }
    }

}
