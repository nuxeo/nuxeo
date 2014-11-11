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

package org.nuxeo.ecm.webapp.security;

import static org.jboss.seam.ScopeType.SESSION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * POJO class that extracts and holds the list of the available permissions from
 * backend.
 *
 * @author Razvan Caraghin
 */
@Name("permissionListManager")
@Scope(SESSION)
public class PermissionListManager implements Serializable {

    private static final long serialVersionUID = -7288271409172281902L;

    private static final Log log = LogFactory.getLog(PermissionListManager.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    protected SelectItem[] availablePermissions;

    protected String selectedPermission;


    public SelectItem[] getAvailablePermissions() throws ClientException {
        if (null == availablePermissions) {
            log.debug("Factory method called...");

            List<SelectItem> jsfModelList = new ArrayList<SelectItem>();

            List<String> permissions = documentManager.getAvailableSecurityPermissions();

            for (String permission : permissions) {
                SelectItem it = new SelectItem(permission);
                jsfModelList.add(it);
            }

            availablePermissions = jsfModelList.toArray(new SelectItem[0]);
        }

        return availablePermissions;
    }

    public String getSelectedPermission() {
        return selectedPermission;
    }

    public void setSelectedPermission(String selectedPermission) {
        this.selectedPermission = selectedPermission;
    }

}
