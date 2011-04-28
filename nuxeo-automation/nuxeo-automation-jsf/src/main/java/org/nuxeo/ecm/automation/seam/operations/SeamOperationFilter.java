package org.nuxeo.ecm.automation.seam.operations;

import javax.servlet.http.HttpServletRequest;

import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.ServletLifecycle;
import org.jboss.seam.core.ConversationPropagation;
import org.jboss.seam.core.Manager;
import org.jboss.seam.web.ServletContexts;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.util.SeamContextHelper;

public class SeamOperationFilter {


    public static void handleBeforeRun(OperationContext context, String conversationId) {

        CoreSession session = context.getCoreSession();
        HttpServletRequest request = (HttpServletRequest) context.get("request");
        ServletLifecycle.beginRequest(request);
        ServletContexts.instance().setRequest(request);

        if (conversationId==null) {
            conversationId = (String) context.get("conversationId");
        }

        if (conversationId!=null) {
            ConversationPropagation.instance().setConversationId( conversationId);
            Manager.instance().restoreConversation();
            ServletLifecycle.resumeConversation(request);
            Contexts.getEventContext().set("documentManager", session);

            ActionContext seamActionContext = new ActionContext();
            NavigationContext navigationContext = (NavigationContext) Contexts.getConversationContext().get("navigationContext");
            seamActionContext.setCurrentDocument(navigationContext.getCurrentDocument());
            seamActionContext.setDocumentManager(session);
            seamActionContext.put("SeamContext", new SeamContextHelper());
            seamActionContext.setCurrentPrincipal((NuxeoPrincipal)session.getPrincipal());

            context.put("seamActionContext", seamActionContext);
        }
    }

    public static void handleAfterRun(OperationContext context, String conversationId) {
        HttpServletRequest request = (HttpServletRequest) context.get("request");

        if (conversationId==null) {
            conversationId = (String) context.get("conversationId");
        }

        if (conversationId!=null) {
            //CoreSession seamDocumentManager = (CoreSession) Contexts.getConversationContext().get("seamDocumentManager");
            Contexts.getEventContext().remove("documentManager");
            //Manager.instance().endConversation(true);
        }
        ServletLifecycle.endRequest(request);
    }
}
