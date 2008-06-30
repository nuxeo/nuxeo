/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.theme.fragment;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ActionService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.util.SeamContextHelper;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.fragments.AbstractFragment;
import org.nuxeo.theme.models.MenuItem;
import org.nuxeo.theme.models.Model;
import org.nuxeo.theme.properties.FieldInfo;

public final class ActionFragment extends AbstractFragment {

    private static final Log log = LogFactory.getLog(ActionFragment.class);

    @FieldInfo(type = "string", label = "category", description = "The action category.")
    public String category = "";

    private final ActionService actionService = (ActionService) Framework.getRuntime().getComponent(
            ActionService.ID);

    public ActionFragment() {
    }

    public ActionFragment(String category) {
        this.category = category;
    }

    @Override
    public Model getModel() {
        ResourcesAccessor resourcesAccessor = (ResourcesAccessor) Component.getInstance("resourcesAccessor");
        Map<String, String> messages = resourcesAccessor.getMessages();

        // Set up the action context
        ActionContext ctx = new ActionContext();
        ctx.put("SeamContext", new SeamContextHelper());

        CoreSession documentManager = (CoreSession) Component.getInstance("documentManager");
        ctx.setDocumentManager(documentManager);
        if (documentManager != null) {
            ctx.setCurrentPrincipal((NuxeoPrincipal) documentManager.getPrincipal());
        }
        NavigationContext navigationContext = (NavigationContext) Component.getInstance(
                "navigationContext");
        if (navigationContext != null) {
            ctx.setCurrentDocument(navigationContext.getCurrentDocument());
        }

        // Create menu items
        MenuItem root = new MenuItem("", "", "", true, "");
        for (Action action : actionService.getActions(category, ctx)) {
            final String label = action.getLabel();
            // FIXME: use the actual link url, not a JSF view id
            final String url = action.getLink();
            root.addChild(new MenuItem(messages.get(label), "", url, true,
                    action.getIcon()));
        }
        return root;
    }
}
