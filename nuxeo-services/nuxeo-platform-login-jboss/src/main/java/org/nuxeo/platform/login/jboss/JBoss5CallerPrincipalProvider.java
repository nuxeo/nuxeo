/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.platform.login.jboss;

import java.util.Set;

import javax.security.auth.Subject;

import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.nuxeo.ecm.core.api.CallerPrincipalProvider;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 * Fix a problem related to EJBContext.getCallerPrincipal() in JBoss5 which
 * return the originated principal and not the authenticated one.
 *
 * See NXP-
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class JBoss5CallerPrincipalProvider extends CallerPrincipalProvider {

    public NuxeoPrincipal getCallerPrincipal() {
        SecurityContext secContext = SecurityContextAssociation.getSecurityContext();
        if (secContext != null) {
            Subject subject = secContext.getUtil().getSubject();
            @SuppressWarnings("rawtypes")
            Set set = subject.getPrincipals(NuxeoPrincipal.class);
            if (set != null && !set.isEmpty()) {
                return (NuxeoPrincipal) set.iterator().next();
            }
        }
        return null;
    }

}
