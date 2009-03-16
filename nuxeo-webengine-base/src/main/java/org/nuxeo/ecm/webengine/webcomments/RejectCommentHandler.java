package org.nuxeo.ecm.webengine.webcomments;

import org.jbpm.graph.exe.ExecutionContext;

public class RejectCommentHandler extends CommentHandlerHelper {

    private static final long serialVersionUID = 1L;

    @Override
    public void execute(ExecutionContext executionContext) throws Exception {
        this.executionContext = executionContext;
        if (nuxeoHasStarted()) {
            if (nuxeoHasStarted()) {
                followTransition("moderation_reject");
            }
        }
        executionContext.getToken().signal();
    }
}
