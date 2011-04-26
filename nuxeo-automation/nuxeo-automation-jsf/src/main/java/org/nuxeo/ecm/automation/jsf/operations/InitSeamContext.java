package org.nuxeo.ecm.automation.jsf.operations;

import javax.servlet.http.HttpServletRequest;

import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.ServletLifecycle;
import org.jboss.seam.core.ConversationPropagation;
import org.jboss.seam.core.Manager;
import org.jboss.seam.web.ServletContexts;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
@Operation(id = InitSeamContext.ID, category = Constants.CAT_UI, label = "Init Seam Context", description = "Initialize a Seam context (including Conversation if needed)")
public class InitSeamContext {

    public static final String ID = "Seam.InitContext";

    @Param(name="conversationId", required=false)
    protected String conversationId;

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext context;

    @OperationMethod
    public void run() throws Exception {

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
            //CoreSession seamDocumentManager = (CoreSession) Contexts.getConversationContext().get("documentManager");
            //Contexts.getConversationContext().set("seamDocumentManager", seamDocumentManager);
            //Contexts.getConversationContext().set("documentManager", session);
        }
    }
}
