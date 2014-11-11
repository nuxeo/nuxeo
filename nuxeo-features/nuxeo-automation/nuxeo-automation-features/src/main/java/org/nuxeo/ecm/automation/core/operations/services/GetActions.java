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
 */

package org.nuxeo.ecm.automation.core.operations.services;

import java.util.List;
import java.util.Locale;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;

/**
 * Queries {@link ActionManager} for available actions in the given context
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
@Operation(id = GetActions.ID, category = Constants.CAT_SERVICES, label = "List available actions", description = "Retrieve list of available actions for a given category. Action context is built based on the Operation context (currentDocument will be fetched from Context if not provided as input). If this operation is executed in a chain that initialized the Seam context, it will be used for Action context")
public class GetActions {

    private static final String SEAM_ACTION_CONTEXT = "seamActionContext";

    public static final String ID = "Actions.GET";

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext ctx;

    @Context
    protected AutomationService automation;

    @Context
    protected ActionManager actionService;

    @Param(name = "category", required = true)
    protected String category;

    @Param(name = "lang", required = false)
    protected String lang;

    protected DocumentModel getCurrentDocumentFromContext() throws Exception {
        String cdRef = (String) ctx.get("currentDocument");
        return automation.getAdaptedValue(ctx, cdRef, DocumentModel.class);
    }

    protected ActionContext getActionContext(DocumentModel currentDocument)
            throws Exception {

        if (ctx.containsKey(SEAM_ACTION_CONTEXT)) {
            // if Seam Context has been initialized, use it
            return (ActionContext) ctx.get(SEAM_ACTION_CONTEXT);
        }
        ActionContext actionContext = new ActionContext();
        actionContext.setDocumentManager(session);
        actionContext.setCurrentPrincipal((NuxeoPrincipal) session.getPrincipal());
        if (currentDocument != null) {
            actionContext.setCurrentDocument(currentDocument);
        } else {
            actionContext.setCurrentDocument(getCurrentDocumentFromContext());
        }
        actionContext.putAll(ctx);
        return actionContext;
    }

    protected Locale getLocale() {
        if (lang == null) {
            lang = (String) ctx.get("lang");
        }
        if (lang == null) {
            lang = "en";
        }
        return new Locale(lang);
    }

    protected String translate(String key) {
        if (key == null) {
            return "";
        }
        return I18NUtils.getMessageString("messages", key, new Object[0],
                getLocale());
    }

    @OperationMethod
    public Blob run() throws Exception {

        return run(null);
    }

    @OperationMethod
    public Blob run(DocumentModel currentDocument) throws Exception {

        ActionContext actionContext = getActionContext(currentDocument);
        List<Action> actions = actionService.getActions(category, actionContext);

        JSONArray rows = new JSONArray();
        for (Action action : actions) {
            JSONObject obj = new JSONObject();

            obj.element("id", action.getId());
            obj.element("link", action.getLink());
            obj.element("icon", action.getIcon());

            String label = translate(action.getLabel());
            obj.element("label", label);
            String help = translate(action.getHelp());
            obj.element("help", help);
            rows.add(obj);
        }
        return new ByteArrayBlob(rows.toString().getBytes("UTF-8"),
                "application/json");
    }

}
