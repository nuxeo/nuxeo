package org.nuxeo.ecm.platform.web.common.requestcontroller.service;

import javax.servlet.http.HttpServletRequest;

/**
 * Interface for the service used to manage what {@link HttpServletRequest}
 * must be protected by the Filter.
 *
 * @author tiry
 */
public interface RequestControllerManager {

    RequestFilterConfig getConfigForRequest(HttpServletRequest request);

}
