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
 * $Id$
 */
package org.nuxeo.ecm.webapp.admin;

import java.io.Serializable;
import java.util.Calendar;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.management.api.AdministrativeStatusManager;
import org.nuxeo.ecm.platform.web.common.admin.AdminStatusHelper;

/**
 * Seam Bean to expose Administrator Message to the JSF Web Layer. (base on {@link AdministrativeStatusManager}
 *
 * @author tiry
 */
@Name("adminMessageManager")
@Scope(ScopeType.APPLICATION)
public class AdminMessageActionBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Factory(value = "adminMessageActivated", scope = ScopeType.EVENT)
    public boolean isAdminMessageActivated() {
        return AdminStatusHelper.isAdminMessageActivated();
    }

    @Factory(value = "adminMessage", scope = ScopeType.EVENT)
    public String getAdminMessage() {
        return AdminStatusHelper.getAdminMessage();
    }

    @Factory(value = "adminMessageModificationDate", scope = ScopeType.EVENT)
    public Calendar getAdminMessageModificationDate() {
        return AdminStatusHelper.getAdminMessageModificationDate();
    }

}
