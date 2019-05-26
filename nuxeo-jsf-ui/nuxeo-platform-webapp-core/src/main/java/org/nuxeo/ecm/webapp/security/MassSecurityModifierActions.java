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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.security;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.List;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
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

    public Boolean getBlockRightInheritance() {
        return blockRightInheritance;
    }

    public void setBlockRightInheritance(Boolean blockRightInheritance) {
        this.blockRightInheritance = blockRightInheritance;
    }

    // Really used?
    public String applySecurity(String listName) {
        // get the list
        List<DocumentModel> docs2Modify = documentsListsManager.getWorkingList(listName);
        for (DocumentModel doc : docs2Modify) {
            if (!documentManager.hasPermission(doc.getParentRef(), SecurityConstants.WRITE_PROPERTIES)) {
                continue;
            }
        }

        /*
         * Object[] params = { nb_published_docs }; facesMessages.add(FacesMessage.SEVERITY_INFO, "#0 " +
         * resourcesAccessor.getMessages().get("n_published_docs"), params); if (nb_published_docs <
         * docs2Publish.size()) { facesMessages.add(FacesMessage.SEVERITY_WARN, resourcesAccessor.getMessages().get(
         * "selection_contains_non_publishable_docs")); }
         */
        // check rights on the lists

        // apply security

        // navigate
        return navigationContext.navigateToDocument(navigationContext.getCurrentDocument());
    }

}
