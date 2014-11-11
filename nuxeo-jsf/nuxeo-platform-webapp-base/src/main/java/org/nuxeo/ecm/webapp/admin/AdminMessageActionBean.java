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
 * $Id$
 */
package org.nuxeo.ecm.webapp.admin;

import java.io.Serializable;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.management.api.AdministrativeStatusManager;
import org.nuxeo.ecm.platform.web.common.admin.AdminStatusHelper;

/**
 *
 * Seam Bean to expose Administrator Message to the JSF Web Layer.
 * (base on {@link AdministrativeStatusManager}
 *
 * @author tiry
 *
 */
@Name("adminMessageManager")
@Scope(ScopeType.APPLICATION)
public class AdminMessageActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Factory(value="adminMessageActivated", scope=ScopeType.EVENT)
    public boolean isAdminMessageActivated() {
        return AdminStatusHelper.isAdminMessageActivated();
    }

    @Factory(value="adminMessage", scope=ScopeType.EVENT)
    public String getAdminMessage() {
        return AdminStatusHelper.getAdminMessage();
    }

}
