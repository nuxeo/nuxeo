package org.nuxeo.ecm.platform.wi.backend;

import org.nuxeo.ecm.platform.wi.filter.WISession;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Organization: Gagnavarslan ehf
 */
public interface BackendFactory {

    Backend getBackend(String path, HttpServletRequest request);

    Backend getBackend(WISession wiSession);

}
