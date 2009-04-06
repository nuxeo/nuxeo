package org.nuxeo.ecm.core.rest;

import java.util.ArrayList;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.jbpm.JbpmListFilter;
import org.nuxeo.ecm.webengine.webcomments.utils.WebCommentsConstants;


public class CommentWorkflowFilter implements JbpmListFilter {

    private static final long serialVersionUID = 1L;

    protected final String commentId;

    public CommentWorkflowFilter(String commentId) {
        super();
        this.commentId = commentId;
    }

    @SuppressWarnings("unchecked")
    public <T> ArrayList<T> filter(JbpmContext jbpmContext,
            DocumentModel document, ArrayList<T> list, NuxeoPrincipal principal) {
        ArrayList<ProcessInstance> result = new ArrayList<ProcessInstance>();
        for (T t : list) {
            ProcessInstance pi = (ProcessInstance) t;
            String name = pi.getProcessDefinition().getName();
            if (WebCommentsConstants.MODERATION_PROCESS.equals(name)) {
                String postId = (String) pi.getContextInstance().getVariable(
                        "postRef");
                if (this.commentId.equals(postId)) {
                    result.add(pi);
                }
            }
        }
        return (ArrayList<T>) result;
    }

}
