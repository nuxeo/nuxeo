package org.nuxeo.ecm.webdav.backend;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Organization: Gagnavarslan ehf
 */
public class Backend {

protected static WebDavBackendFactory factoryWebDav = null;

    private static final Log log = LogFactory.getLog(WebDavBackendFactory.class);

    public static WebDavBackendFactory getFactory() {
        if (factoryWebDav == null) {
            factoryWebDav = loadFactory();
        }
        return factoryWebDav;
    }

    protected synchronized static WebDavBackendFactory loadFactory() {
        String factoryClass = "org.nuxeo.ecm.platform.wi.backend.webdav.WebDavBackendFactoryImpl";
        try {
            factoryWebDav = (WebDavBackendFactory) Class.forName(factoryClass, true, Thread.currentThread().getContextClassLoader()).newInstance();
        } catch (Exception e) {
            log.error("Unable to create backend factoryWebDav", e);
        }
        return factoryWebDav;
    }

    public static WebDavBackend get(String path, HttpServletRequest request) {
        return getFactory().getBackend(path, request);
    }
    
}
