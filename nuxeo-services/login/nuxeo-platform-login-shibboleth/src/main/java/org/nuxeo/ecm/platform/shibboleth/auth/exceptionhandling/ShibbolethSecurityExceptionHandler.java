/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.shibboleth.auth.exceptionhandling;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.web.Session;
import org.nuxeo.ecm.platform.shibboleth.service.ShibbolethAuthenticationService;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.webapp.exceptionhandling.NuxeoSecurityExceptionHandler;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class ShibbolethSecurityExceptionHandler extends NuxeoSecurityExceptionHandler {

    private static final Log log = LogFactory.getLog(ShibbolethSecurityExceptionHandler.class);

    @Override
    protected boolean handleAnonymousException(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        if (getService() == null) {
            return false;
        }
        String loginURL = getService().getLoginURL(request);
        if (loginURL == null) {
            log.error("Unable to handle Shibboleth login, no loginURL registered");
            return false;
        }
        try {
            if (!response.isCommitted()) {
                request.setAttribute(NXAuthConstants.DISABLE_REDIRECT_REQUEST_KEY, true);
                Session.instance().invalidate();
                response.sendRedirect(loginURL);
                responseComplete();
            } else {
                log.error("Cannot redirect to login page: response is already commited");
            }
        } catch (IOException e) {
            String errorMessage = String.format("Unable to handle Shibboleth login on %s", loginURL);
            log.error(errorMessage, e);
        }
        return true;
    }

    protected ShibbolethAuthenticationService getService() {
        return Framework.getService(ShibbolethAuthenticationService.class);
    }

}
