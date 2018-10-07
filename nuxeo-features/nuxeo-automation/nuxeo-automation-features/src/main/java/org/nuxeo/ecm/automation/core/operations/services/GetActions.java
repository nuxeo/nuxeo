/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.automation.core.operations.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.LocaleUtils;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ELActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;

/**
 * Queries {@link ActionManager} for available actions in the given context
 *
 * @author Tiry (tdelprat@nuxeo.com)
 */
@Operation(id = GetActions.ID, category = Constants.CAT_SERVICES, label = "List available actions", description = "Retrieve list of available actions for a given category. Action context is built based on the Operation context (currentDocument will be fetched from Context if not provided as input). If this operation is executed in a chain that initialized the Seam context, it will be used for Action context", addToStudio = false)
public class GetActions {

    public static final String SEAM_ACTION_CONTEXT = "seamActionContext";

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

    protected DocumentModel getCurrentDocumentFromContext() throws OperationException {
        String cdRef = (String) ctx.get("currentDocument");
        return automation.getAdaptedValue(ctx, cdRef, DocumentModel.class);
    }

    protected ActionContext getActionContext(DocumentModel currentDocument) throws OperationException {
        if (ctx.containsKey(SEAM_ACTION_CONTEXT)) {
            // if Seam Context has been initialized, use it
            return (ActionContext) ctx.get(SEAM_ACTION_CONTEXT);
        }
        ActionContext actionContext = new ELActionContext();
        actionContext.setDocumentManager(session);
        actionContext.setCurrentPrincipal(session.getPrincipal());
        if (currentDocument != null) {
            actionContext.setCurrentDocument(currentDocument);
        } else {
            actionContext.setCurrentDocument(getCurrentDocumentFromContext());
        }
        actionContext.putAllLocalVariables(ctx);
        return actionContext;
    }

    protected Locale getLocale() {
        if (lang == null) {
            lang = (String) ctx.get("lang");
        }
        if (lang == null) {
            lang = "en";
        }
        return LocaleUtils.toLocale(lang);
    }

    protected String translate(String key) {
        if (key == null) {
            return "";
        }
        return I18NUtils.getMessageString("messages", key, new Object[0], getLocale());
    }

    @OperationMethod
    public Blob run() throws IOException, OperationException {
        return run(null);
    }

    @OperationMethod
    public Blob run(DocumentModel currentDocument) throws IOException, OperationException {

        ActionContext actionContext = getActionContext(currentDocument);
        List<Action> actions = actionService.getActions(category, actionContext);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Action action : actions) {
            Map<String, Object> obj = new LinkedHashMap<>();

            obj.put("id", action.getId());
            obj.put("link", action.getLink());
            obj.put("icon", action.getIcon());

            String label = translate(action.getLabel());
            obj.put("label", label);
            String help = translate(action.getHelp());
            obj.put("help", help);

            Map<String, Object> properties = new LinkedHashMap<>(action.getProperties());
            obj.put("properties", properties);
            rows.add(obj);
        }
        return Blobs.createJSONBlobFromValue(rows);
    }
}
