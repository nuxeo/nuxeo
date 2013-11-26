/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *    Mariana Cedica
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.routing.web;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.List;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.webapp.action.ActionContextProvider;
import org.nuxeo.ecm.webapp.helpers.EventNames;


/**
 * Web Actions to edit a document route
 *
 * @author Mariana Cedica
 */
@Name("routingWebActions")
@Scope(CONVERSATION)
@Install(precedence = Install.FRAMEWORK)
public class DocumentRoutingWebActionsBean implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @In(create = true, required = false)
    protected transient WebActions webActions;

    @In(create = true, required = false)
    protected transient ActionContextProvider actionContextProvider;

    protected List<Action> addStepActions;

    protected List<Action> addStepInForkActions;

    protected List<Action> removeStepActions;

    protected List<Action> editStepActions;

    public List<Action> getAddStepActions(DocumentModel step) {
        ActionContext context = actionContextProvider.createActionContext();
        context.setCurrentDocument(step);
        addStepActions = webActions.getActionsList(DocumentRoutingWebConstants.ADD_STEP_ACTIONS_LIST, context);
        return addStepActions;
    }

    public List<Action> getAddStepInActions(DocumentModel step) {
        ActionContext context = actionContextProvider.createActionContext();
        context.setCurrentDocument(step);
        addStepInForkActions = webActions.getActionsList(DocumentRoutingWebConstants.ADD_STEP_IN_FORK_ACTIONS_LIST, context);
        return addStepInForkActions;
    }

    public List<Action> getRemoveStepActions(DocumentModel step) {
        ActionContext context = actionContextProvider.createActionContext();
        context.setCurrentDocument(step);
        removeStepActions = webActions.getActionsList(DocumentRoutingWebConstants.REMOVE_STEP_ACTIONS_LIST, context);
        return removeStepActions;
    }

    public List<Action> getEditStepActions(DocumentModel step) {
        ActionContext context = actionContextProvider.createActionContext();
        context.setCurrentDocument(step);
        editStepActions = webActions.getActionsList(DocumentRoutingWebConstants.EDIT_STEP_ACTIONS_LIST, context);
        return editStepActions;
    }

    @Observer(value = { EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED,
            EventNames.LOCATION_SELECTION_CHANGED }, create = false)
    @BypassInterceptors
    public void resetTabList() {
        addStepActions = null;
        removeStepActions = null;
        addStepInForkActions = null;
        editStepActions = null;
    }

}
