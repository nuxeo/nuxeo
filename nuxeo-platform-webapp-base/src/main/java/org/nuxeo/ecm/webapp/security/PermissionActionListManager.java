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
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.runtime.api.Framework;

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

    /**
     * Framework property to control whether negative ACLs (deny) are allowed.
     *
     * @since 5.9.6
     */
    public static final String ALLOW_NEGATIVE_ACL_PROPERTY = "nuxeo.security.allowNegativeACL";

    protected String selectedGrant = "Grant";

    @In(create = true)
    private transient ResourcesAccessor resourcesAccessor;

    /**
     * Returns true if negative ACLs are allowed.
     *
     * @since 5.9.6
     * @see #ALLOW_NEGATIVE_ACL_PROPERTY
     */
    public boolean getAllowNegativeACL() {
        return Framework.isBooleanPropertyTrue(ALLOW_NEGATIVE_ACL_PROPERTY);
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
