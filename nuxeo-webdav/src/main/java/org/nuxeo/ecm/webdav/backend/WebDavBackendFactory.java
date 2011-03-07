package org.nuxeo.ecm.webdav.backend;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Organization: Gagnavarslan ehf
 */
public interface WebDavBackendFactory {

    WebDavBackend getBackend(String path, HttpServletRequest request);

}
