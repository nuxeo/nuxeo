package org.nuxeo.ecm.automation.seam.operations;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationParameters;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;

@Operation(id = RunOperationInSeam.ID, category = Constants.CAT_UI, label = "Run operation in Seam Context", description = "Initialize a Seam context (including Conversation if needed) and runs an Operation")
public class RunOperationInSeam {

    public static final String ID = "Seam.RunOperation";

    @Context
    protected OperationContext ctx;

    @Param(name="conversationId", required=false)
    protected String conversationId;

    @Context
    protected AutomationService service;

    @Param(name = "id")
    protected String chainId;

    @Param(name="isolate", required = false, values = "false")
    protected boolean isolate = false;

    @OperationMethod
    public Object run() throws Exception {

        Map<String, Object> vars = isolate ? new HashMap<String, Object>(ctx.getVars()) : ctx.getVars();

        OperationContext subctx = new OperationContext(ctx.getCoreSession(), vars);
        subctx.setInput(ctx.getInput());
        SeamOperationFilter.handleBeforeRun(ctx, conversationId);
        try {
            if (chainId.startsWith("Chain.")) {
                return service.run(subctx, chainId.substring(6));
            } else {
                OperationChain chain = new OperationChain("operation");
                Map<String, Object> caller_params = (Map<String, Object>) vars.remove("caller_params");
                if (caller_params!=null) {
                    vars.putAll(caller_params);
                }
                OperationParameters oparams = new OperationParameters(chainId,vars);
                chain.add(oparams);

                return service.run(subctx, chain);
            }
        } finally {
            SeamOperationFilter.handleAfterRun(ctx, conversationId);
        }

    }
}
