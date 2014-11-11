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
package org.nuxeo.ecm.webengine.security.guards;

import java.security.Principal;

import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.webengine.model.Access;
import org.nuxeo.ecm.webengine.security.Guard;
import org.nuxeo.runtime.model.Adaptable;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("isAdministrator")
public class IsAdministratorGuard implements Guard {

    protected boolean isAdministrator;

    public IsAdministratorGuard() {
        isAdministrator = true;
    }

    public IsAdministratorGuard(String isAdministrator) {
        this.isAdministrator = Boolean.parseBoolean(isAdministrator);
    }

    public IsAdministratorGuard(Access isAdministrator) {
        this.isAdministrator = isAdministrator == Access.GRANT;
    }

    public boolean check(Adaptable context) {
        Principal p = context.getAdapter(Principal.class);
        if (p instanceof NuxeoPrincipal) {
            return ((NuxeoPrincipal)p).isAdministrator() == isAdministrator;
        }
        return false;
    }

    public boolean isAdministrator() {
        return isAdministrator;
    }

    public String toString() {
        return "IS_ADMINISTRATOR[" + isAdministrator + "]";
    }

}
