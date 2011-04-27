package org.nuxeo.ecm.automation.jsf.operations;

import javax.servlet.http.HttpServletRequest;

import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.ServletLifecycle;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;

@Operation(id = DestroySeamContext.ID, category = Constants.CAT_UI, label = "Destroy Seam Context", description = "Cleanup up Seam context")
public class DestroySeamContext {

    public static final String ID = "Seam.DestroyContext";

    @Param(name="conversationId", required=false)
    protected String conversationId;

    @Context
    protected OperationContext context;

    @OperationMethod
    public void run() throws Exception {

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
