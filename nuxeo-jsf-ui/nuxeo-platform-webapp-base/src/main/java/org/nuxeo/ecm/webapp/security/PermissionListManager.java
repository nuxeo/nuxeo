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
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * POJO class that extracts and holds the list of the available permissions from backend.
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

    public SelectItem[] getAvailablePermissions() {
        if (null == availablePermissions) {
            log.debug("Factory method called...");

            List<SelectItem> jsfModelList = new ArrayList<>();

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
