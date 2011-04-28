package org.nuxeo.ecm.automation.seam.operations;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
@Operation(id = InitSeamContext.ID, category = Constants.CAT_UI, label = "Init Seam Context", description = "Initialize a Seam context (including Conversation if needed)")
public class InitSeamContext {

    public static final String ID = "Seam.InitContext";

    @Param(name="conversationId", required=false)
    protected String conversationId;

    @Context
    protected OperationContext context;

    @OperationMethod
    public void run() throws Exception {
        SeamOperationFilter.handleBeforeRun(context, conversationId);
    }
}
