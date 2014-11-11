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
        return documentManager == null ? false
                : documentManager.isNegativeAclAllowed();
    }

    public SelectItem[] getPermissionActionItems() {
        List<String> permissionActions = new ArrayList<String>();
        List<SelectItem> jsfModelList = new ArrayList<SelectItem>();

        permissionActions.add("Grant");

        if (getAllowNegativeACL()) {
            permissionActions.add("Deny");
        }

        for (String permissionAction : permissionActions) {
            String label = labeler.makeLabel(permissionAction);
            SelectItem it = new SelectItem(permissionAction,
                    resourcesAccessor.getMessages().get(label));
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
