package org.nuxeo.ecm.platform.ui.web.auth.interfaces;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpSession;

/**
 * SessionManager interface for Authentication Filter.
 *
 * @author tiry
 */
public interface NuxeoAuthenticationSessionManager {

    boolean bypassRequest(ServletRequest request);

    /**
     * Used to know if SessionManager is available for a given request.
     */
    boolean isAvalaible(ServletRequest request);

    /**
     * Destroys web session and associated resources.
     */
    void invalidateSession(ServletRequest request);

    /**
     * Reinitializes a Session.
     */
    HttpSession reinitSession(ServletRequest request);


}
