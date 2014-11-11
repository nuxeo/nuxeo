/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: UserSessionBean.java 30577 2008-02-26 13:46:19Z ogrisel $
 */

package org.nuxeo.ecm.webapp.security;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;
import java.security.Principal;

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

    private Principal currentUser;

    private static final Log log = LogFactory.getLog(UserSessionBean.class);

    @Factory(value = "currentUser", scope = SESSION)
    public Principal getCurrentUser() {
        if (currentUser == null) {
            FacesContext fContext = FacesContext.getCurrentInstance();
            if (fContext == null) {
                currentUser = null;
                log.error(
                        "Can not fetch user principal from FacesContext: "
                        + "there is no FacesContext attached to the current request");
            } else {
        	// if seam identify filter is available, we can not get the UserPrincipal directly from the request
                //currentUser = ((HttpServletRequest)((HttpServletRequestWrapper)(fContext.getExternalContext().getRequest())).getRequest()).getUserPrincipal();
                currentUser = fContext.getExternalContext().getUserPrincipal();
            }
        }
        return currentUser;
    }

    @Factory(value = "currentNuxeoPrincipal", scope = SESSION)
    public NuxeoPrincipal getCurrentNuxeoPrincipal() {
        return (NuxeoPrincipal) getCurrentUser();
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
