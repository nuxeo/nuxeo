/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: UserSessionBean.java 30577 2008-02-26 13:46:19Z ogrisel $
 */

package org.nuxeo.ecm.webapp.security;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

@Startup
@Name("userSession")
@Scope(SESSION)
public class UserSessionBean implements Serializable, UserSession {

    private static final long serialVersionUID = 7639281445209754L;

    private NuxeoPrincipal currentUser;

    private static final Log log = LogFactory.getLog(UserSessionBean.class);

    @Factory(value = "currentUser", scope = SESSION)
    public NuxeoPrincipal getCurrentUser() {
        if (currentUser == null) {
            FacesContext fContext = FacesContext.getCurrentInstance();
            if (fContext == null) {
                currentUser = null;
                log.error("Can not fetch user principal from FacesContext: "
                        + "there is no FacesContext attached to the current request");
            } else {
                // if seam identify filter is available, we can not get the UserPrincipal directly from the request
                // currentUser =
                // ((HttpServletRequest)((HttpServletRequestWrapper)(fContext.getExternalContext().getRequest())).getRequest()).getUserPrincipal();
                currentUser = (NuxeoPrincipal) fContext.getExternalContext().getUserPrincipal();
            }
        }
        return currentUser;
    }

    @Factory(value = "currentNuxeoPrincipal", scope = SESSION)
    public NuxeoPrincipal getCurrentNuxeoPrincipal() {
        return getCurrentUser();
    }

    public boolean isAdministrator() {
        NuxeoPrincipal user = getCurrentNuxeoPrincipal();
        if (user == null) {
            return false;
        } else {
            return user.isAdministrator();
        }
    }

    @Destroy
    public void destroy() {
    }

}
