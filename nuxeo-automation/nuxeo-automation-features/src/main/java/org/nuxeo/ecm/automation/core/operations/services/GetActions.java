package org.nuxeo.ecm.automation.core.operations.services;

import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

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
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ActionService;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;

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

    protected ResourceBundle getTranslationBundle() {
        if (lang!=null) {
            Locale locale = new Locale(lang);
            return ResourceBundle.getBundle("messages",locale);
        } else {
            return ResourceBundle.getBundle("messages");
        }
    }

    @OperationMethod
    public Blob run() throws Exception {
        return run(null);
    }

    @OperationMethod
    public Blob run(DocumentModel currentDocument) throws Exception {

        ActionContext actionContext = getActionContext(currentDocument);
        List<Action> actions = actionService.getActions(category, actionContext);

        ResourceBundle messages = getTranslationBundle();

        JSONArray rows = new JSONArray();
        for (Action action : actions) {
            JSONObject obj = new JSONObject();

            obj.element("id", action.getId());
            obj.element("link", action.getLink());
            obj.element("icon", action.getIcon());

            String label = null;
            try {
                label = messages.getString(action.getLabel());
            } catch (MissingResourceException e) {
                label = action.getLabel();
            }
            obj.element("label", label);
            String help = null;
            try {
                help = messages.getString(action.getHelp());
            } catch (MissingResourceException e) {
                help = action.getHelp();
            }
            obj.element("help", help);
            rows.add(obj);
        }
        return new StringBlob(rows.toString(), "application/json");
    }

}
