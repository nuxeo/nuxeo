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

package org.nuxeo.ecm.webapp.contentbrowser;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.ADD_CHILDREN;
import static org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_CHANGED;
import static org.nuxeo.ecm.webapp.helpers.EventNames.LOCATION_SELECTION_CHANGED;
import static org.nuxeo.ecm.webapp.helpers.EventNames.NEW_DOCUMENT_CREATED;

import java.io.Serializable;
import java.util.List;

import javax.annotation.security.PermitAll;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remove;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModel;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelImpl;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelRowEvent;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListDescriptor;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.querymodel.QueryModelActions;

/**
 * Action listener that deals with operations with the domains.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
@Name("domainActions")
@Scope(CONVERSATION)
public class DomainActionsBean extends InputController implements DomainActions, Serializable {

    private static final long serialVersionUID = 4151925677321848635L;

    private static final Log log = LogFactory.getLog(DomainActionsBean.class);

    private static final String QM_USER_DOMAINS = "USER_DOMAINS";

    // disabled: see comment in getDomains implementation
    // @In(create = true)
    // private NavigationContext navigationContext;

    @In(create = true)
    private transient QueryModelActions queryModelActions;

    @In(create = true, required = false)
    private transient CoreSession documentManager;

    @In(create = true)
    private transient DocumentsListsManager documentsListsManager;

    // do not try to (un)serialize a list of ResultDocumentModel as it's
    // probably as they are fetched automatically if null
    private transient DocumentModelList domains;

    public DocumentModelList getDomains() throws ClientException {
        if (domains == null) {
            try {
                // TODO: OG: we currently use the search service instead of
                // CoreSession.getChildren to workaround a limitation of the
                // current ACL
                // implementation that forces inheritance by default
                // see http://jira.nuxeo.org/browse/NXP-1293 for details

                // DocumentModel root = documentManager.getRootDocument();
                // FacetFilter facetFilter = new
                // FacetFilter("HiddenInNavigation",
                // false);
                // return documentManager.getChildren(root.getRef(), null,
                // SecurityConstants.READ, facetFilter, null);
                domains = queryModelActions.get(QM_USER_DOMAINS).getDocuments(
                        documentManager, new Object[0]);
            } catch (Throwable t) {
                throw ClientException.wrap(t);
            }
        }
        return domains;
    }

    @Observer( value= { LOCATION_SELECTION_CHANGED, NEW_DOCUMENT_CREATED, DOCUMENT_CHANGED },
            create=false)
    @BypassInterceptors
    public void invalidateDomainList() {
        domains = null;
    }

    public Boolean getCanAddDomains() throws ClientException {
        return documentManager.hasPermission(
                documentManager.getRootDocument().getRef(), ADD_CHILDREN);
    }

    public SelectDataModel getDomainsSelectModel() throws ClientException {
        List<DocumentModel> selectedDomains = documentsListsManager.getWorkingList(DOMAINS_WORKING_LIST);
        SelectDataModel model = new SelectDataModelImpl(DOMAINS_WORKING_LIST,
                getDomains(), selectedDomains);
        model.addSelectModelListener(this);
        // XXX AT: see if cache is useful
        // cacheUpdateNotifier.addCacheListener(model);
        return model;
    }

    public void processSelectRowEvent(SelectDataModelRowEvent event) {
        Boolean selection = event.getSelected();
        DocumentModel data = (DocumentModel) event.getRowData();
        if (selection) {
            // create only if needed
            if (documentsListsManager.getWorkingList(DOMAINS_WORKING_LIST) == null) {
                documentsListsManager.createWorkingList(DOMAINS_WORKING_LIST,
                        new DocumentsListDescriptor(DOMAINS_WORKING_LIST));
            }
            documentsListsManager.addToWorkingList(DOMAINS_WORKING_LIST, data);
        } else {
            documentsListsManager.removeFromWorkingList(DOMAINS_WORKING_LIST,
                    data);
        }
    }

    //@Create
    public void initialize() {
        log.debug("Initializing...");
    }

    @Destroy
    @Remove
    @PermitAll
    public void destroy() {
        log.debug("Removing Seam action listener...");
    }

    @PrePassivate
    public void saveState() {
        log.debug("PrePassivate");
    }

    @PostActivate
    public void readState() {
        log.debug("PostActivate");
    }

}
