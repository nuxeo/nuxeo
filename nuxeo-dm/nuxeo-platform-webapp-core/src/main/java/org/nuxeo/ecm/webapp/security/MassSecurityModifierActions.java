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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.security;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.List;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;

@Name("massSecurityModifierActions")
@Scope(CONVERSATION)
public class MassSecurityModifierActions implements Serializable {

    private static final long serialVersionUID = 4978984433628773791L;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    protected transient DocumentsListsManager documentsListsManager;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    private Boolean blockRightInheritance;

    @In(create = true)
    private PermissionActionListManager permissionActionListManager;

    @In(create = true)
    private PermissionListManager permissionListManager;

    @In(create = true)
    private PrincipalListManager principalListManager;

    private final SecurityData securityData = null;


    public Boolean getBlockRightInheritance() {
        return blockRightInheritance;
    }

    public void setBlockRightInheritance(Boolean blockRightInheritance) {
        this.blockRightInheritance = blockRightInheritance;
    }

    // Really used?
    public String applySecurity(String listName) throws ClientException {
        // get the list
        List<DocumentModel> docs2Modify = documentsListsManager.getWorkingList(listName);
        int nbModifiedDocs = 0;

        for (DocumentModel doc : docs2Modify) {
            if (!documentManager.hasPermission(doc.getParentRef(),
                    SecurityConstants.WRITE_PROPERTIES)) {
                continue;
            }
        }

        /*
        Object[] params = { nb_published_docs };
        facesMessages.add(FacesMessage.SEVERITY_INFO, "#0 "
                + resourcesAccessor.getMessages().get("n_published_docs"),
                params);

        if (nb_published_docs < docs2Publish.size())
        {
            facesMessages.add(FacesMessage.SEVERITY_WARN,
                resourcesAccessor.getMessages().get(
                        "selection_contains_non_publishable_docs"));
        }
*/
        // check rights on the lists


        // apply security


        // navigate
        return navigationContext.navigateToDocument(navigationContext.getCurrentDocument());
    }

}
