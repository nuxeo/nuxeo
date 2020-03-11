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

import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.USERIDENT_KEY;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.shibboleth.service.ShibbolethAuthenticationService;
import org.nuxeo.ecm.platform.ui.web.auth.CachableUserIdentificationInfo;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.DefaultNuxeoExceptionHandler;
import org.nuxeo.runtime.api.Framework;

import java.security.Principal;
import java.util.Optional;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class ShibbolethSecurityExceptionHandler extends DefaultNuxeoExceptionHandler {

    private static final Log log = LogFactory.getLog(ShibbolethSecurityExceptionHandler.class);

    @Override
    public String getLoginURL(HttpServletRequest request) {
        ShibbolethAuthenticationService shibService = Framework.getService(ShibbolethAuthenticationService.class);
        if (shibService == null) {
            return null;
        }
        String loginURL = shibService.getLoginURL(request);
        if (loginURL == null) {
            log.error("Unable to handle Shibboleth login, no loginURL registered");
            return null;
        }
        return loginURL;
    }

    @Override
    protected Principal getPrincipal(HttpServletRequest request) {
        Principal principal = super.getPrincipal(request);
        if (principal == null) {
            principal = Optional.ofNullable(request.getSession(false))
                                .map(s -> (CachableUserIdentificationInfo) s.getAttribute(USERIDENT_KEY))
                                .map(CachableUserIdentificationInfo::getPrincipal)
                                .orElse(null);
        }
        return principal;
    }

}
