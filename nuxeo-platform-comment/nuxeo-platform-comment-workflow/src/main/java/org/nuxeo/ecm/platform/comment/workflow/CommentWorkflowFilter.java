/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     mcedica
 */
package org.nuxeo.ecm.platform.comment.workflow;

import java.util.ArrayList;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.jbpm.JbpmListFilter;
import org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants;


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
            if (CommentsConstants.MODERATION_PROCESS.equals(name)) {
                String commentId = (String) pi.getContextInstance().getVariable(
                        CommentsConstants.COMMENT_ID);
                if (this.commentId.equals(commentId)) {
                    result.add(pi);
                }
            }
        }
        return (ArrayList<T>) result;
    }

}
