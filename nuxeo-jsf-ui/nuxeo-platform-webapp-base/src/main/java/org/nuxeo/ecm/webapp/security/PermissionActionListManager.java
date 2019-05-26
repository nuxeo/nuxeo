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

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.common.utils.i18n.Labeler;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * Lists the available permission actions. Hardcoded. ATM Grant/Deny supported.
 *
 * @author Razvan Caraghin
 */
@Name("permissionActionListManager")
@Scope(SESSION)
public class PermissionActionListManager implements Serializable {

    private static final long serialVersionUID = -327848199566592785L;

    private static final Labeler labeler = new Labeler("label.security");

    protected String selectedGrant = "Grant";

    @In(create = true)
    private transient ResourcesAccessor resourcesAccessor;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    /**
     * Returns true if negative ACLs are allowed.
     *
     * @since 6.0
     */
    public boolean getAllowNegativeACL() {
        return documentManager == null ? false : documentManager.isNegativeAclAllowed();
    }

    public SelectItem[] getPermissionActionItems() {
        List<String> permissionActions = new ArrayList<>();
        List<SelectItem> jsfModelList = new ArrayList<>();

        permissionActions.add("Grant");

        if (getAllowNegativeACL()) {
            permissionActions.add("Deny");
        }

        for (String permissionAction : permissionActions) {
            String label = labeler.makeLabel(permissionAction);
            SelectItem it = new SelectItem(permissionAction, resourcesAccessor.getMessages().get(label));
            jsfModelList.add(it);
        }

        SelectItem[] permissionActionItems = jsfModelList.toArray(new SelectItem[0]);
        return permissionActionItems;
    }

    public String getSelectedGrant() {
        return selectedGrant;
    }

    public void setSelectedGrant(String selectedPermission) {
        selectedGrant = selectedPermission;
    }

}
