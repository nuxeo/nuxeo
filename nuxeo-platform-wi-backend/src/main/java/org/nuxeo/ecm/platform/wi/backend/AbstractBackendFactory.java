package org.nuxeo.ecm.platform.wi.backend;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.wi.filter.WIRequestFilter;
import org.nuxeo.ecm.platform.wi.filter.WISession;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Organization: Gagnavarslan ehf
 */
public abstract class AbstractBackendFactory implements BackendFactory {

    private static final Log log = LogFactory.getLog(AbstractBackendFactory.class);

    public Backend getBackend(String path, HttpServletRequest request) {

        if (log.isDebugEnabled() && request != null) {
            log.debug("Get backend for method:" + request.getMethod() + " uri:" + request.getRequestURI());
        }

        Backend backend = null;
        if (request != null) {
            WISession wiSession = (WISession) request.getAttribute(WIRequestFilter.SESSION_KEY);
            backend = getBackend(wiSession);
        } else {
            //for tests
            backend = createRootBackend();
        }
        return backend.getBackend(path);
    }

    public Backend getBackend(WISession wiSession) {

        Backend backend = null;
        if (wiSession != null) {
            Object sessionBackend = wiSession.getAttribute(WISession.BACKEND_KEY);
            if (sessionBackend != null) {
                backend = (Backend) sessionBackend;
            }
        }

        if (backend == null) {
            CoreSession session = null;
            if (wiSession != null) {
                session = (CoreSession) wiSession.getAttribute(WISession.CORESESSION_KEY);
            }
            backend = createRootBackend();
            if (session != null) {
                backend.setSession(session);
            }
        }

        if (wiSession != null) {
            wiSession.setAttribute(WISession.BACKEND_KEY, backend);
        }

        return backend;
    }

    protected abstract Backend createRootBackend();

}
