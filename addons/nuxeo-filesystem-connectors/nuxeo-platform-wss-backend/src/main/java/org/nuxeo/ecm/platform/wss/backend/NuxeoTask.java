/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */

package org.nuxeo.ecm.platform.wss.backend;

import java.util.Date;
import java.util.List;

import org.jbpm.graph.exe.Comment;
import org.jbpm.taskmgmt.exe.PooledActor;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.nuxeo.ecm.platform.jbpm.JbpmService.TaskVariableName;
import org.nuxeo.wss.servlet.WSSRequest;
import org.nuxeo.wss.spi.dws.AbstractTask;
import org.nuxeo.wss.spi.dws.Task;

public class NuxeoTask extends AbstractTask implements Task {

    protected TaskInstance jbpmTask;

    protected String body;

    protected String directive;

    protected String assigneeLogin;

    public NuxeoTask(TaskInstance jbpmTask, String link) {
        super(new Long(jbpmTask.getId()).toString(),
                jbpmTask.getActorId(),
                jbpmTask.getCreate(),
                jbpmTask.getCreate(),
                link);
        this.jbpmTask = jbpmTask;
        this.directive = (String) jbpmTask.getVariableLocally(TaskVariableName.directive.name());

        //String creator = null;
        List<Comment> comments = jbpmTask.getComments();
        StringBuffer commentBuffer = new StringBuffer();
        if (jbpmTask.getDescription()!=null) {
            commentBuffer.append(jbpmTask.getDescription());
            commentBuffer.append("\n\n");
        }
        if (comments != null && !comments.isEmpty()) {
            for (Comment comment: comments) {
                String commentAuthor =null;
                if (comment.getActorId()!=null) {
                    commentAuthor = comment.getActorId();
                    if (commentAuthor.contains(":")) {
                        commentAuthor = commentAuthor.split(":")[1];
                    }
                }
                if (authorLogin==null) {
                    authorLogin = commentAuthor;
                }
                commentBuffer.append(commentAuthor);
                commentBuffer.append(" : ");
                AbstractTask.getDateFormat().format(comment.getTime());
                commentBuffer.append("\n");
                commentBuffer.append(comment.getMessage());
                commentBuffer.append("^n");
            }
        }
        //authorId = authorLogin;
        body = commentBuffer.toString();

        for (Object actor : jbpmTask.getPooledActors()) {
            String id = ((PooledActor) actor).getActorId();
            if (id.contains(":")) {
                id = id.split(":")[1];
            }
            if (assigneeLogin==null && id!=null) {
                assigneeLogin = id;
                break;
            }
        }
    }

    public String getAssigneeLogin() {
        return assigneeLogin;
    }

    public String getBody() {
        return body;
    }

    public Date getDueDate() {
        return jbpmTask.getDueDate();
    }

    public String getPriority() {
        int priority = jbpmTask.getPriority();
        return "(2) Normal";
    }

    public String getStatus() {
        return "In Progress";
    }

    public String getTitle() {
        return directive;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void translateDirective(WSSRequest request) {
        directive = TranslationHelper.getLabel(directive, request);
    }

}
