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

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.faces.context.FacesContext;

import org.jboss.annotation.ejb.SerializedConcurrentAccess;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

@Startup
@Stateful
@Name("userSession")
@Scope(SESSION)
@SerializedConcurrentAccess
public class UserSessionBean implements Serializable, UserSession {

    private static final long serialVersionUID = 7639281445209754L;

    private Principal currentUser;

    @Resource
    transient EJBContext context;

    @Factory(value = "currentUser", scope = SESSION)
    public Principal getCurrentUser() throws Exception {
        if (currentUser == null) {
            if (FacesContext.getCurrentInstance() != null) {
                currentUser = FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();
            } else {
                return context.getCallerPrincipal();
            }
        }
        return currentUser;
    }

    @Factory(value = "currentNuxeoPrincipal", scope = SESSION)
    public NuxeoPrincipal getCurrentNuxeoPrincipal() throws Exception {
        return (NuxeoPrincipal) getCurrentUser();
    }

    public boolean isAdministrator() throws Exception {
        return getCurrentNuxeoPrincipal().isAdministrator();
    }

    @Destroy
    @Remove
    public void destroy() {
    }

}
