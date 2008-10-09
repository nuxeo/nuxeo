package org.nuxeo.ecm.platform.ui.web.auth.interfaces;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Define SessionManager interface for Authentication Filter
 *
 * @author tiry
 *
 */
public interface NuxeoAuthenticationSessionManager {


    boolean bypassRequest(ServletRequest request);

    /**
     *
     * Used to know if SessionManager is available for a given request
     *
     * @param request
     * @return
     */
    boolean isAvalaible(ServletRequest request);

    /**
     * destroy web session and associated resources
     *
     */
    void invalidateSession(ServletRequest request);


    /**
     *
     * Reinitialize a Session
     *
     * @param session
     */
    HttpSession reinitSession(ServletRequest request);


}
