package org.nuxeo.ecm.platform.wi.backend.webdav;

import org.nuxeo.ecm.platform.wi.backend.Backend;
import org.nuxeo.ecm.platform.wi.backend.BackendFactory;
import org.nuxeo.ecm.platform.wi.service.PluggableBackendFactory;
import org.nuxeo.ecm.webdav.backend.WebDavBackend;
import org.nuxeo.ecm.webdav.backend.WebDavBackendFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Organization: Gagnavarslan ehf
 */
public class WebDavBackendFactoryImpl implements WebDavBackendFactory {

    private BackendFactory factory = new PluggableBackendFactory();

    @Override
    public WebDavBackend getBackend(String path, HttpServletRequest httpServletRequest) {
        Backend backend = factory.getBackend(path, httpServletRequest);
        if(backend == null){
            return null;
        }
        return new WebDavBackendAdapter(backend);
    }
}
