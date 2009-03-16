package org.nuxeo.ecm.webengine.webcomments;

import org.jbpm.graph.exe.ExecutionContext;

public class AcceptCommentHandler extends CommentHandlerHelper {

    private static final long serialVersionUID = 1L;

    @Override
    public void execute(ExecutionContext executionContext) throws Exception {
        this.executionContext = executionContext;
        if (nuxeoHasStarted()) {
            followTransition("moderation_publish");
        }
        executionContext.getToken().signal();
    }
}
