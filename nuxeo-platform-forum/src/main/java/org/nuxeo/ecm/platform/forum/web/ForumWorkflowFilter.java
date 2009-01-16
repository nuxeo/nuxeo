/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.forum.web;

import java.util.ArrayList;

import org.jbpm.JbpmContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.forum.workflow.ForumConstants;
import org.nuxeo.ecm.platform.jbpm.JbpmListFilter;
import org.nuxeo.ecm.platform.jbpm.JbpmService;

/**
 * Filters forum moderation process instances if they apply to given post.
 *
 * <p>
 * Filter on the thread document model is handled by the {@link JbpmService}.
 *
 * @author Anahide Tchertchian
 */
public class ForumWorkflowFilter implements JbpmListFilter {

    private static final long serialVersionUID = 1L;

    protected final String postId;

    public ForumWorkflowFilter(String postId) {
        super();
        this.postId = postId;
    }

    @SuppressWarnings("unchecked")
    public <T> ArrayList<T> filter(JbpmContext jbpmContext,
            DocumentModel document, ArrayList<T> list, NuxeoPrincipal principal) {
        ArrayList<ProcessInstance> result = new ArrayList<ProcessInstance>();
        for (T t : list) {
            ProcessInstance pi = (ProcessInstance) t;
            String name = pi.getProcessDefinition().getName();
            if (ForumConstants.PROCESS_INSTANCE_NAME.equals(name)) {
                String postId = (String) pi.getContextInstance().getVariable(
                        ForumConstants.POST_REF);
                if (this.postId.equals(postId)) {
                    result.add(pi);
                }
            }
        }
        return (ArrayList<T>) result;
    }

}
