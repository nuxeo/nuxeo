package org.nuxeo.ecm.platform.wss.service;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.wss.servlet.WSSRequest;
import org.nuxeo.wss.spi.WSSBackend;
import org.nuxeo.wss.spi.WSSBackendFactory;

public class PluggableBackendFactory implements WSSBackendFactory {

    public WSSBackend getBackend(WSSRequest request) {
        WSSPlugableBackendManager manager = (WSSPlugableBackendManager) Framework.getRuntime().getComponent(WSSPlugableBackendManager.NAME);
        WSSBackendFactory factory = manager.getBackendFactory();
        return factory.getBackend(request);
    }

}
